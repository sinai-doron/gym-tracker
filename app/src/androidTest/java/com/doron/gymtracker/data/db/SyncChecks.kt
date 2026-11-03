package com.doron.gymtracker.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * What sync needs from a database at the current version, checked the same way after a migration
 * and on a fresh install: every synced row has a uuid and a change-log entry, and the triggers
 * are exactly the ones a fresh install gets. The v11/v12 triggers passed plain-SQL tests and
 * still crashed the app (see SyncMigration.log), so a migration must end with the same triggers.
 */
object SyncChecks {

    /** Table -> the kind sync_changes logs it as, including the music log added in version 12. */
    val syncedTables: Map<String, String> = SyncMigration.TABLES + ("track_log" to "song")

    /** Tables with rows that have no uuid, and how many; empty when every row has one. */
    fun rowsWithoutUuid(db: SupportSQLiteDatabase): Map<String, Long> =
        syncedTables.keys.associateWith { db.long("SELECT count(*) FROM `$it` WHERE uuid IS NULL") }
            .filterValues { it > 0 }

    /** Kind -> rows that should have a change-log entry (each item once, plus one per route or session with points). */
    fun expectedChanges(db: SupportSQLiteDatabase): Map<String, Long> =
        syncedTables.entries.associate { (table, kind) -> kind to db.long("SELECT count(*) FROM `$table`") } +
            mapOf(
                "route_points" to db.long("SELECT count(DISTINCT routeId) FROM route_points"),
                "track" to db.long("SELECT count(DISTINCT sessionId) FROM location_log"),
                SyncMigration.LIBRARY_EXERCISE to db.long("SELECT count(*) FROM exercises WHERE source = 'CUSTOM'"),
            )

    /** Kind -> change-log entries waiting to be uploaded. */
    fun loggedChanges(db: SupportSQLiteDatabase): Map<String, Long> =
        (syncedTables.values + listOf("route_points", "track", SyncMigration.LIBRARY_EXERCISE)).associateWith {
            db.long("SELECT count(*) FROM sync_changes WHERE entity = '$it' AND deleted = 0")
        }

    /** Trigger name -> its SQL, for the sync triggers only. */
    fun triggers(db: SupportSQLiteDatabase): Map<String, String> =
        db.query("SELECT name, sql FROM sqlite_master WHERE type = 'trigger' AND name LIKE 'sync_%'").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }

    fun applying(db: SupportSQLiteDatabase): Long = db.long("SELECT applying FROM sync_control WHERE id = 1")

    /** The triggers a fresh install gets, from an in-memory database created the way the app creates one. */
    fun freshInstallTriggers(context: Context): Map<String, String> {
        val fresh = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = GymDatabase.installFresh(db)
            })
            .build()
        return try {
            triggers(fresh.openHelper.writableDatabase)
        } finally {
            fresh.close()
        }
    }

    private fun SupportSQLiteDatabase.long(sql: String): Long = query(sql).use { it.moveToFirst(); it.getLong(0) }
}
