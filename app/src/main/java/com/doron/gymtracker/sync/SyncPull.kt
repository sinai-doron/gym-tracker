package com.doron.gymtracker.sync

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** One item from the server, as /api/gym/sync/pull returns it. */
data class IncomingChange(
    val entity: String,
    val id: String,
    val updatedAt: Long,
    val deleted: Boolean,
    val data: JsonObject?,
)

/**
 * Download: writes server changes into the local database, the reverse of [SyncPayloads].
 *
 * - Runs with `sync_control.applying` set, so the change log doesn't record these writes as local
 *   changes to send back.
 * - The newer change wins: a local change not yet uploaded and newer than the server's is kept
 *   (it goes up next); otherwise the server's replaces it, and the local entry is dropped.
 * - Items refer to each other by uuid. One whose parent hasn't arrived yet (a set before its
 *   entry) waits in `sync_inbox` and is tried again after every page.
 * - The training plan matches by `key` first, so a fresh install's own copy of the plan takes the
 *   server's uuids instead of being duplicated.
 * - A session's GPS track arrives in chunks, kept in `sync_track_parts`; the session's points are
 *   rebuilt from all of them whenever one changes.
 */
class SyncPull(private val db: SupportSQLiteDatabase) {

    fun ensureTables() {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS sync_inbox (entity TEXT NOT NULL, id TEXT NOT NULL, updated_at INTEGER NOT NULL, " +
                "deleted INTEGER NOT NULL, data TEXT, PRIMARY KEY (entity, id))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS sync_track_parts (id TEXT PRIMARY KEY, session_uuid TEXT NOT NULL, " +
                "chunk INTEGER NOT NULL, points TEXT NOT NULL)"
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_cursor (id INTEGER PRIMARY KEY CHECK (id = 1), seq INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS sync_track_chunks (session_uuid TEXT PRIMARY KEY, chunks INTEGER NOT NULL)")
    }

    fun cursor(): Long = db.query("SELECT seq FROM sync_cursor WHERE id = 1").use { if (it.moveToFirst()) it.getLong(0) else 0 }

    /**
     * Applies one page and moves the cursor to [next], all in one transaction: a crash part-way
     * leaves both as they were, and the page is pulled again.
     */
    fun applyPage(changes: List<IncomingChange>, next: Long) {
        db.beginTransaction()
        try {
            db.execSQL("UPDATE sync_control SET applying = 1 WHERE id = 1")
            changes.sortedBy { ORDER.indexOf(it.entity).let { i -> if (i < 0) ORDER.size else i } }
                .forEach { change -> if (!apply(change)) defer(change) }
            retryInbox()
            db.execSQL("INSERT OR REPLACE INTO sync_cursor (id, seq) VALUES (1, ?)", arrayOf<Any?>(next))
            db.execSQL("UPDATE sync_control SET applying = 0 WHERE id = 1")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** Items whose parents have arrived since. Repeats while any of them goes in. */
    private fun retryInbox() {
        while (true) {
            val waiting = db.query("SELECT entity, id, updated_at, deleted, data FROM sync_inbox").use { c ->
                buildList {
                    while (c.moveToNext()) add(
                        IncomingChange(
                            c.getString(0), c.getString(1), c.getLong(2), c.getInt(3) != 0,
                            c.getString(4)?.let { SyncJson.parseToJsonElement(it) as JsonObject },
                        )
                    )
                }
            }
            val done = waiting.filter { apply(it) }
            done.forEach { db.execSQL("DELETE FROM sync_inbox WHERE entity = ? AND id = ?", arrayOf<Any?>(it.entity, it.id)) }
            if (done.isEmpty()) return
        }
    }

    private fun defer(change: IncomingChange) {
        db.execSQL(
            "INSERT OR REPLACE INTO sync_inbox VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(change.entity, change.id, change.updatedAt, if (change.deleted) 1 else 0, change.data?.toString()),
        )
    }

    /** False when it has to wait for a parent. */
    private fun apply(change: IncomingChange): Boolean {
        if (change.entity !in DERIVED && localIsNewer(change)) return true
        val data = change.data
        if (change.deleted || data == null) {
            deleteLocal(change)
            return true
        }
        return when (change.entity) {
            "workout" -> upsertPlanItem("workouts", change.id, data.str("key"), ContentValues().apply {
                put("label", data.str("label"))
            })
            "exercise" -> {
                val workoutId = idOf("workouts", data.str("workoutId")) ?: return false
                val variantOf = data.strOrNull("variantOfId")?.let { idOf("exercise_definitions", it) ?: return false }
                upsertPlanItem("exercise_definitions", change.id, data.strOrNull("key"), ContentValues().apply {
                    put("workoutId", workoutId)
                    put("name", data.str("name"))
                    put("type", data.str("type"))
                    put("targetSets", data.int("targetSets"))
                    put("restDurationSecs", data.int("restDurationSecs"))
                    putIntOrNull("targetReps", data.intOrNull("targetReps"))
                    putIntOrNull("targetDistanceM", data.intOrNull("targetDistanceM"))
                    put("isOptional", data.bool("isOptional"))
                    put("isPerSide", data.bool("isPerSide"))
                    put("isBodyweight", data.bool("isBodyweight"))
                    put("variantOfId", variantOf)
                    put("sortOrder", data.int("sortOrder"))
                    put("isRemoved", data.bool("isRemoved"))
                })
            }
            "session" -> {
                val type = localSessionType(data.str("type"))
                val workoutId = data.strOrNull("workoutId")?.let { idOf("workouts", it) ?: return false } ?: type
                upsert("sessions", change.id, ContentValues().apply {
                    put("workoutId", workoutId)
                    put("startedAt", data.long("startedAt"))
                    putLongOrNull("finishedAt", data.longOrNull("finishedAt"))
                    put("type", type)
                    putDoubleOrNull("distanceMeters", data.doubleOrNull("distanceMeters"))
                })
            }
            "entry" -> {
                val sessionId = numericIdOf("sessions", data.str("sessionId")) ?: return false
                val exerciseId = idOf("exercise_definitions", data.str("exerciseId")) ?: return false
                upsert("exercise_entries", change.id, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("exerciseId", exerciseId)
                })
            }
            "set" -> {
                val entryId = numericIdOf("exercise_entries", data.str("entryId")) ?: return false
                upsert("set_records", change.id, ContentValues().apply {
                    put("entryId", entryId)
                    put("completedAt", data.long("completedAt"))
                    put("type", data.str("type"))
                    putIntOrNull("reps", data.intOrNull("reps"))
                    putDoubleOrNull("weightKg", data.doubleOrNull("weightKg"))
                    putIntOrNull("durationSecs", data.intOrNull("durationSecs"))
                    putIntOrNull("distanceM", data.intOrNull("distanceM"))
                })
            }
            "run" -> {
                val sessionId = numericIdOf("sessions", data.str("sessionId")) ?: return false
                // Keyed by its session locally: the run of a session is one row whatever its uuid.
                db.execSQL("DELETE FROM run_details WHERE sessionId = ? AND uuid != ?", arrayOf<Any?>(sessionId, change.id))
                upsert("run_details", change.id, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("targetType", data.str("targetType"))
                    putDoubleOrNull("targetValue", data.doubleOrNull("targetValue"))
                    put("lapMeters", data.int("lapMeters"))
                    putIntOrNull("steps", data.intOrNull("steps"))
                    putDoubleOrNull("elevationGainM", data.doubleOrNull("elevationGainM"))
                    putIntOrNull("calories", data.intOrNull("calories"))
                    putIntOrNull("weatherTempC", data.intOrNull("weatherTempC"))
                    put("weatherCondition", data.strOrNull("weatherCondition"))
                })
            }
            "pause" -> {
                val sessionId = numericIdOf("sessions", data.str("sessionId")) ?: return false
                upsert("run_pauses", change.id, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("startedAt", data.long("startedAt"))
                    putLongOrNull("endedAt", data.longOrNull("endedAt"))
                })
            }
            "route" -> upsert("routes", change.id, ContentValues().apply {
                put("name", data.str("name"))
                put("distanceMeters", data.double("distanceMeters"))
                put("createdAt", data.long("createdAt"))
            })
            "route_points" -> {
                val routeId = numericIdOf("routes", data.str("routeId")) ?: return false
                db.execSQL("DELETE FROM route_points WHERE routeId = ?", arrayOf<Any?>(routeId))
                data.getValue("points").jsonArray.forEachIndexed { seq, p ->
                    val point = p.jsonArray
                    db.insert("route_points", SQLiteDatabase.CONFLICT_REPLACE, ContentValues().apply {
                        put("routeId", routeId)
                        put("seq", seq)
                        put("latitude", point[0].double())
                        put("longitude", point[1].double())
                        putDoubleOrNull("elevationM", point[2].doubleOrNullValue())
                    })
                }
                true
            }
            SyncPayloads.TRACK -> {
                val sessionUuid = data.str("sessionId")
                numericIdOf("sessions", sessionUuid) ?: return false
                db.execSQL(
                    "INSERT OR REPLACE INTO sync_track_parts VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(change.id, sessionUuid, data.int("chunk"), data.getValue("points").toString()),
                )
                rebuildTrack(sessionUuid)
                true
            }
            "song" -> {
                val sessionId = numericIdOf("sessions", data.str("sessionId")) ?: return false
                upsert("track_log", change.id, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("startedAt", data.long("startedAt"))
                    put("title", data.str("title"))
                    put("artist", data.str("artist"))
                })
            }
            else -> true // a kind this version doesn't know: nothing to do with it
        }
    }

    /** A local change not uploaded yet, made after this one, wins; otherwise it is dropped. */
    private fun localIsNewer(change: IncomingChange): Boolean {
        val local = db.query(
            "SELECT changed_at FROM sync_changes WHERE entity = ? AND uuid = ?", arrayOf<Any?>(change.entity, change.id)
        ).use { if (it.moveToFirst()) it.getLong(0) else null } ?: return false
        if (local > change.updatedAt) return true
        db.execSQL("DELETE FROM sync_changes WHERE entity = ? AND uuid = ?", arrayOf<Any?>(change.entity, change.id))
        return false
    }

    private fun deleteLocal(change: IncomingChange) {
        when (change.entity) {
            "route_points" -> db.execSQL(
                "DELETE FROM route_points WHERE routeId IN (SELECT id FROM routes WHERE uuid = ?)", arrayOf<Any?>(change.id)
            )
            SyncPayloads.TRACK -> {
                val session = db.query("SELECT session_uuid FROM sync_track_parts WHERE id = ?", arrayOf<Any?>(change.id))
                    .use { if (it.moveToFirst()) it.getString(0) else null } ?: return
                db.execSQL("DELETE FROM sync_track_parts WHERE id = ?", arrayOf<Any?>(change.id))
                rebuildTrack(session)
            }
            else -> TABLE_OF[change.entity]?.let { table ->
                db.execSQL("DELETE FROM `$table` WHERE uuid = ?", arrayOf<Any?>(change.id))
            }
        }
    }

    /** The session's GPS points, from every chunk the server has sent, in order. */
    private fun rebuildTrack(sessionUuid: String) {
        val sessionId = numericIdOf("sessions", sessionUuid) ?: return
        val parts = db.query(
            "SELECT points FROM sync_track_parts WHERE session_uuid = ? ORDER BY chunk", arrayOf<Any?>(sessionUuid)
        ).use { c -> buildList { while (c.moveToNext()) add(SyncJson.parseToJsonElement(c.getString(0)).jsonArray) } }
        db.execSQL("DELETE FROM location_log WHERE sessionId = ?", arrayOf<Any?>(sessionId))
        parts.forEach { points ->
            points.forEach { p ->
                val point = p.jsonArray
                db.insert("location_log", SQLiteDatabase.CONFLICT_NONE, ContentValues().apply {
                    put("sessionId", sessionId)
                    put("timestamp", point[0].jsonPrimitive.longOrNull ?: 0)
                    put("latitude", point[1].double())
                    put("longitude", point[2].double())
                    putDoubleOrNull("speed", point[3].doubleOrNullValue())
                    putDoubleOrNull("altitude", point[4].doubleOrNullValue())
                    putDoubleOrNull("accuracy", point[5].doubleOrNullValue())
                    putDoubleOrNull("bearing", point[6].doubleOrNullValue())
                })
            }
        }
        // What the server holds for this session's track: kept in step for uploads.
        db.execSQL("INSERT OR REPLACE INTO sync_track_chunks VALUES (?, ?)", arrayOf<Any?>(sessionUuid, parts.size))
    }

    /** By uuid, else by the plan's own key; a new item takes its key as its local id. */
    private fun upsertPlanItem(table: String, uuid: String, key: String?, values: ContentValues): Boolean {
        values.put("uuid", uuid)
        if (db.update(table, SQLiteDatabase.CONFLICT_ABORT, values, "uuid = ?", arrayOf<Any?>(uuid)) > 0) return true
        if (key != null) {
            val oldUuid = db.query("SELECT uuid FROM `$table` WHERE id = ?", arrayOf<Any?>(key))
                .use { if (it.moveToFirst()) it.getString(0) else null }
            if (db.update(table, SQLiteDatabase.CONFLICT_ABORT, values, "id = ?", arrayOf<Any?>(key)) > 0) {
                // A fresh install's own copy of the plan takes the server's uuid; the upload its
                // old uuid was waiting for is no longer wanted.
                if (oldUuid != null && oldUuid != uuid) {
                    db.execSQL("DELETE FROM sync_changes WHERE uuid = ?", arrayOf<Any?>(oldUuid))
                }
                return true
            }
        }
        values.put("id", key ?: uuid)
        db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
        return true
    }

    private fun upsert(table: String, uuid: String, values: ContentValues): Boolean {
        values.put("uuid", uuid)
        if (db.update(table, SQLiteDatabase.CONFLICT_ABORT, values, "uuid = ?", arrayOf<Any?>(uuid)) == 0) {
            db.insert(table, SQLiteDatabase.CONFLICT_ABORT, values)
        }
        return true
    }

    private fun idOf(table: String, uuid: String): String? =
        db.query("SELECT id FROM `$table` WHERE uuid = ?", arrayOf<Any?>(uuid)).use { if (it.moveToFirst()) it.getString(0) else null }

    private fun numericIdOf(table: String, uuid: String): Long? =
        db.query("SELECT id FROM `$table` WHERE uuid = ?", arrayOf<Any?>(uuid)).use { if (it.moveToFirst()) it.getLong(0) else null }

    companion object {
        /** Parents before children, so most items go in on the first try. */
        private val ORDER = listOf(
            "workout", "exercise", "route", "session", "entry", "set", "run", "pause", "song", "route_points", SyncPayloads.TRACK,
        )
        private val DERIVED = setOf("route_points", SyncPayloads.TRACK)
        private val TABLE_OF = mapOf(
            "workout" to "workouts", "exercise" to "exercise_definitions", "session" to "sessions",
            "entry" to "exercise_entries", "set" to "set_records", "run" to "run_details",
            "pause" to "run_pauses", "route" to "routes", "song" to "track_log",
        )

        fun localSessionType(server: String): String = when (server) {
            "RUN" -> "RUNNING"
            "WALK" -> "WALKING"
            else -> "GYM"
        }
    }
}

internal val SyncJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

private fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content
private fun JsonObject.strOrNull(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
private fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.longOrNull ?: 0
private fun JsonObject.longOrNull(key: String): Long? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.longOrNull
private fun JsonObject.int(key: String): Int = long(key).toInt()
private fun JsonObject.intOrNull(key: String): Int? = longOrNull(key)?.toInt()
private fun JsonObject.double(key: String): Double = getValue(key).jsonPrimitive.doubleOrNull ?: 0.0
private fun JsonObject.doubleOrNull(key: String): Double? = this[key]?.takeUnless { it is JsonNull }?.jsonPrimitive?.doubleOrNull
private fun JsonObject.bool(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
private fun JsonElement.double(): Double = jsonPrimitive.doubleOrNull ?: 0.0
private fun JsonElement.doubleOrNullValue(): Double? = if (this is JsonNull) null else jsonPrimitive.doubleOrNull

private fun ContentValues.putIntOrNull(key: String, v: Int?) = if (v == null) putNull(key) else put(key, v)
private fun ContentValues.putLongOrNull(key: String, v: Long?) = if (v == null) putNull(key) else put(key, v)
private fun ContentValues.putDoubleOrNull(key: String, v: Double?) = if (v == null) putNull(key) else put(key, v)

