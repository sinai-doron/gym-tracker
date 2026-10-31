package com.doron.gymtracker.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doron.gymtracker.account.Account
import com.doron.gymtracker.account.AuthRepository
import com.doron.gymtracker.account.BackendApi
import com.doron.gymtracker.account.BackendException
import com.doron.gymtracker.account.SyncConsent
import com.doron.gymtracker.data.db.GymDatabase
import com.doron.gymtracker.data.db.SyncMigration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncStatusStore by preferencesDataStore(name = "sync_status")
private val LAST_SUCCESS = longPreferencesKey("last_success")
private val LAST_ERROR = stringPreferencesKey("last_error")

data class SyncStatus(val lastSuccess: Long?, val lastError: String?)

/**
 * Upload: sends what `sync_changes` says changed to doron-backend's /api/gym, in batches the
 * backend accepts (at most 200 changes, well under its 100 KB body limit), and clears each entry
 * the server answered for. Only for a signed-in user whose consent the server has; route data
 * (routes, their points, GPS tracks) only with the routes consent.
 *
 * An entry changed again while its batch was in flight is not cleared, so it goes next time.
 * Download comes separately.
 */
@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: GymDatabase,
    private val api: BackendApi,
    private val auth: AuthRepository,
    private val account: Account,
    private val consent: SyncConsent,
) {
    private val lock = Mutex()

    val status: Flow<SyncStatus> = context.syncStatusStore.data.map { SyncStatus(it[LAST_SUCCESS], it[LAST_ERROR]) }

    sealed interface Outcome {
        data object NotSignedIn : Outcome
        data object NoConsent : Outcome
        data class Synced(val pulled: Int, val pushed: Int) : Outcome
        /** Worth trying again later: offline, rate limited, the server unavailable. */
        data class Retry(val reason: String) : Outcome
        data class Failed(val reason: String) : Outcome
    }

    /**
     * Download first, then upload: a fresh install takes the server's ids for its plan before it
     * uploads anything, and a local change newer than the server's survives the download.
     */
    suspend fun sync(): Outcome = lock.withLock {
        if (auth.currentUser() == null) return@withLock Outcome.NotSignedIn
        val agreed = consent.current() ?: return@withLock Outcome.NoConsent
        // Belt and braces with the check at sign-in: never sync one person's data into another's account.
        if (!account.mayReadAndWriteThisPhonesData()) return@withLock Outcome.Failed("another account owns this phone's data")
        val outcome = try {
            // The backend refuses every push until it has the consent.
            if (!account.uploadConsent()) Outcome.Retry("consent not uploaded")
            else {
                val pulled = pullAll()
                Outcome.Synced(pulled, pushAll(agreed.routes, agreed.music))
            }
        } catch (e: BackendException.Unauthorized) {
            Outcome.NotSignedIn
        } catch (e: BackendException.Network) {
            Outcome.Retry("network")
        } catch (e: BackendException.RateLimited) {
            Outcome.Retry("rate limited")
        } catch (e: BackendException.Conflict) {
            Outcome.Retry("consent required")
        } catch (e: BackendException.Http) {
            if (e.code >= 500) Outcome.Retry("HTTP ${e.code}") else Outcome.Failed("HTTP ${e.code}")
        }
        record(outcome)
        outcome
    }

    /** Every page after the cursor, each applied with its cursor in one transaction. */
    private suspend fun pullAll(): Int = withContext(Dispatchers.IO) {
        val puller = SyncPull(database.openHelper.writableDatabase)
        puller.ensureTables()
        var pulled = 0
        while (true) {
            val since = puller.cursor()
            val page = auth.withAccessToken { token ->
                api.get<PullPage>("/api/gym/sync/pull?since=$since&limit=$PULL_PAGE", token)
            }
            puller.applyPage(page.changes.map { IncomingChange(it.entity, it.id, it.updatedAt, it.deleted, it.data) }, page.next)
            pulled += page.changes.size
            if (!page.more || page.next == since) break
        }
        // Written underneath Room: tell its observers, so open screens show what arrived.
        if (pulled > 0) database.invalidationTracker.refreshVersionsAsync()
        pulled
    }

    private suspend fun pushAll(routesAllowed: Boolean, musicAllowed: Boolean): Int = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_track_chunks (session_uuid TEXT PRIMARY KEY, chunks INTEGER NOT NULL)")
        val payloads = SyncPayloads(db)
        var sent = 0
        while (true) {
            val pending = pending(routesAllowed, musicAllowed)
            if (pending.isEmpty()) break
            val batch = mutableListOf<Pair<Pending, List<OutgoingChange>>>()
            var bytes = 0
            var count = 0
            for (entry in pending) {
                val changes = payloads.changes(entry.entity, entry.uuid, entry.changedAt, entry.deleted)
                val size = changes.sumOf { estimate(it) }
                if (size > MAX_BATCH_BYTES) {
                    // A long run's track: its chunks go in requests of their own, and the entry is
                    // cleared only once every one of them is in. A single item too big on its own
                    // (only a very long imported route could be) is left for a later version.
                    if (changes.size > 1 && changes.all { estimate(it) <= MAX_BATCH_BYTES }) {
                        if (batch.isEmpty()) {
                            sent += pushInParts(db, payloads, entry, changes)
                            continue
                        }
                        break
                    }
                    skip(entry)
                    continue
                }
                if (batch.isNotEmpty() && (bytes + size > MAX_BATCH_BYTES || count + changes.size > MAX_BATCH_CHANGES)) break
                batch += entry to changes
                bytes += size
                count += changes.size
            }
            if (batch.isEmpty()) break

            val body = PushBody(batch.flatMap { (_, changes) -> changes.map { it.toWire() } })
            auth.withAccessToken { token -> api.post<PushBody, PushResult>("/api/gym/sync/push", body, token) }

            // Every change in the batch was answered: applied, stale (the server has newer) or
            // rejected (no routes consent, or a clock far ahead). None needs sending again.
            batch.forEach { (entry, changes) ->
                if (entry.entity == SyncPayloads.TRACK) {
                    payloads.recordSentChunks(entry.uuid, changes.count { !it.deleted })
                }
                db.execSQL(
                    "DELETE FROM sync_changes WHERE entity = ? AND uuid = ? AND changed_at = ?",
                    arrayOf(entry.entity, entry.uuid, entry.changedAt),
                )
            }
            sent += body.changes.size
        }
        sent
    }

    /** One entry's changes over several requests, each under the size limit; then clears it. */
    private suspend fun pushInParts(
        db: androidx.sqlite.db.SupportSQLiteDatabase,
        payloads: SyncPayloads,
        entry: Pending,
        changes: List<OutgoingChange>,
    ): Int {
        val parts = mutableListOf<MutableList<OutgoingChange>>()
        var bytes = 0
        for (change in changes) {
            val size = estimate(change)
            if (parts.isEmpty() || bytes + size > MAX_BATCH_BYTES || parts.last().size >= MAX_BATCH_CHANGES) {
                parts += mutableListOf<OutgoingChange>()
                bytes = 0
            }
            parts.last() += change
            bytes += size
        }
        for (part in parts) {
            val body = PushBody(part.map { it.toWire() })
            auth.withAccessToken { token -> api.post<PushBody, PushResult>("/api/gym/sync/push", body, token) }
        }
        if (entry.entity == SyncPayloads.TRACK) payloads.recordSentChunks(entry.uuid, changes.count { !it.deleted })
        db.execSQL(
            "DELETE FROM sync_changes WHERE entity = ? AND uuid = ? AND changed_at = ?",
            arrayOf(entry.entity, entry.uuid, entry.changedAt),
        )
        return changes.size
    }

    private data class Pending(val entity: String, val uuid: String, val changedAt: Long, val deleted: Boolean)

    /** Oldest first. Without its consent, route or music data waits, except its deletions. */
    private fun pending(routesAllowed: Boolean, musicAllowed: Boolean): List<Pending> {
        val db = database.openHelper.writableDatabase
        val waiting = (if (routesAllowed) emptySet() else SyncPayloads.ROUTE_KINDS) +
            (if (musicAllowed) emptySet() else SyncPayloads.MUSIC_KINDS)
        val routeFilter = if (waiting.isEmpty()) "" else
            "WHERE deleted = 1 OR entity NOT IN (${waiting.joinToString { "'$it'" }})"
        return db.query(
            "SELECT entity, uuid, changed_at, deleted FROM sync_changes $routeFilter ORDER BY changed_at LIMIT $PAGE"
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(Pending(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3) != 0))
            }
        }.filterNot { it in skipped }
    }

    private val skipped = mutableSetOf<Pending>()
    private fun skip(entry: Pending) { skipped += entry }

    /**
     * Turning the routes consent back on: route data already sent was deleted from the server
     * when it was turned off, so all of it is queued again.
     */
    suspend fun requeueRouteData() = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        val now = System.currentTimeMillis()
        db.execSQL("INSERT OR REPLACE INTO sync_changes SELECT 'route', uuid, ?, 0 FROM routes WHERE uuid IS NOT NULL", arrayOf(now))
        db.execSQL(
            "INSERT OR REPLACE INTO sync_changes SELECT 'route_points', r.uuid, ?, 0 FROM routes r " +
                "WHERE r.uuid IS NOT NULL AND EXISTS (SELECT 1 FROM route_points p WHERE p.routeId = r.id)",
            arrayOf(now),
        )
        db.execSQL(
            "INSERT OR REPLACE INTO sync_changes SELECT 'track', s.uuid, ?, 0 FROM sessions s " +
                "WHERE s.uuid IS NOT NULL AND EXISTS (SELECT 1 FROM location_log l WHERE l.sessionId = s.id)",
            arrayOf(now),
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_track_chunks (session_uuid TEXT PRIMARY KEY, chunks INTEGER NOT NULL)")
        db.execSQL("DELETE FROM sync_track_chunks")
    }

    /**
     * Erases everything this user synced from the server (DELETE /api/gym/data), then signs out.
     * The phone keeps its data. Its sync bookkeeping restarts, so a later sign-in uploads it all
     * again rather than only what changed since.
     */
    suspend fun deleteServerDataAndSignOut() = lock.withLock {
        auth.withAccessToken { token -> api.delete("/api/gym/data", token) }
        account.signOut()
        withContext(Dispatchers.IO) {
            val db = database.openHelper.writableDatabase
            SyncPull(db).ensureTables()
            db.execSQL("DELETE FROM sync_cursor")
            db.execSQL("DELETE FROM sync_track_chunks")
            db.execSQL("DELETE FROM sync_track_parts")
            db.execSQL("DELETE FROM sync_inbox")
            val now = System.currentTimeMillis()
            SyncMigration.TABLES.forEach { (table, kind) ->
                db.execSQL("INSERT OR REPLACE INTO sync_changes SELECT '$kind', uuid, ?, 0 FROM `$table` WHERE uuid IS NOT NULL", arrayOf(now))
            }
            db.execSQL("INSERT OR REPLACE INTO sync_changes SELECT 'song', uuid, ?, 0 FROM track_log WHERE uuid IS NOT NULL", arrayOf(now))
        }
        requeueRouteData()
        context.syncStatusStore.edit { it.clear() }
    }

    /** Turning music back on: the server deleted the songs when it was turned off. */
    suspend fun requeueMusic() = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO sync_changes SELECT 'song', uuid, ?, 0 FROM track_log WHERE uuid IS NOT NULL",
            arrayOf(System.currentTimeMillis()),
        )
    }

    private suspend fun record(outcome: Outcome) {
        context.syncStatusStore.edit {
            when (outcome) {
                is Outcome.Synced -> { it[LAST_SUCCESS] = System.currentTimeMillis(); it.remove(LAST_ERROR) }
                is Outcome.Retry -> it[LAST_ERROR] = outcome.reason
                is Outcome.Failed -> it[LAST_ERROR] = outcome.reason
                else -> Unit
            }
        }
    }

    private fun estimate(change: OutgoingChange): Int = 120 + (change.data?.toString()?.length ?: 0)

    @Serializable
    private data class WireChange(
        val entity: String,
        val id: String,
        val updatedAt: Long,
        val deleted: Boolean,
        val data: JsonObject? = null,
    )

    @Serializable
    private data class PullPage(val changes: List<WireChange>, val next: Long, val more: Boolean)

    @Serializable
    private data class PushBody(val changes: List<WireChange>)

    @Serializable
    private data class PushResult(val stale: List<Key> = emptyList(), val rejected: List<Rejected> = emptyList())

    @Serializable
    private data class Key(val entity: String, val id: String)

    @Serializable
    private data class Rejected(val entity: String, val id: String, val reason: String)

    // A deletion carries no data at all: the backend refuses `data` on one.
    private fun OutgoingChange.toWire() = WireChange(entity, id, updatedAt, deleted, if (deleted) null else data)

    private companion object {
        const val MAX_BATCH_BYTES = 80_000
        const val MAX_BATCH_CHANGES = 200
        const val PAGE = 400
        const val PULL_PAGE = 200
    }
}
