package com.doron.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 11: what sync needs from the database, without changing how the app writes to it.
 *
 * - Every synced row gets a `uuid`, the id the server knows it by. Local numeric ids are unique on
 *   one phone only; a second phone, or a restore, would reuse them. Existing rows get theirs now,
 *   new rows from a trigger, so no code that creates rows has to change.
 * - `sync_changes` records, per item, that it changed and when, or that it was deleted. Triggers
 *   on every synced table write it, so no write path can forget to. This is the "updatedAt" and
 *   "deleted" of each item: a deletion leaves a row here instead of a soft-deleted row in place.
 * - `sync_control.applying` is set while changes pulled from the server are written, so they are
 *   not logged back as local changes.
 * - Everything already on the phone is logged once, so the first sync uploads it all.
 *
 * Route points and GPS points are synced per route and per session (`route_points`, `track`),
 * keyed by the route's or session's uuid. The music log (track_log) joined in version 12.
 */
object SyncMigration {

    /** A random (version 4) UUID, in SQL; evaluated per row. */
    const val NEW_UUID = "lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || " +
        "substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (abs(random()) % 4), 1) || " +
        "substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6)))"

    private const val NOW_MS = "CAST((julianday('now') - 2440587.5) * 86400000 AS INTEGER)"
    private const val NOT_APPLYING = "(SELECT applying FROM sync_control WHERE id = 1) = 0"

    /** Table -> the kind the server knows it as. */
    val TABLES = linkedMapOf(
        "workouts" to "workout",
        "exercise_definitions" to "exercise",
        "sessions" to "session",
        "exercise_entries" to "entry",
        "set_records" to "set",
        "run_details" to "run",
        "run_pauses" to "pause",
        "routes" to "route",
    )

    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements().forEach(db::execSQL)
        }
    }

    /**
     * Version 12: the music log (track_log) syncs too, with a consent of its own (decided
     * 2026-10-31). Same as the tables in version 11: a uuid, the triggers, and every existing row
     * logged once. Nothing uploads without the music consent; the sync filters it.
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            tableStatements("track_log", "song").forEach(db::execSQL)
            // The plan went up once without its keys (workout_a, an exercise's id); send it again
            // with them, so a fresh install restoring from the server can match it.
            db.execSQL("INSERT OR REPLACE INTO sync_changes SELECT 'workout', uuid, $NOW_MS, 0 FROM workouts WHERE uuid IS NOT NULL")
            db.execSQL("INSERT OR REPLACE INTO sync_changes SELECT 'exercise', uuid, $NOW_MS, 0 FROM exercise_definitions WHERE uuid IS NOT NULL")
        }
    }

    /**
     * A database created fresh at the current version never runs the migrations above: Room makes
     * the tables (with their uuid columns) from the entities, and this adds the rest, the change
     * log and every trigger, with nothing to backfill. Without it a fresh install would never
     * record a change, nor have the tables a download writes. Idempotent.
     */
    fun installFresh(db: SupportSQLiteDatabase) {
        freshStatements().forEach(db::execSQL)
    }

    fun freshStatements(): List<String> = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS sync_changes (entity TEXT NOT NULL, uuid TEXT NOT NULL, " +
                "changed_at INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY (entity, uuid))"
        )
        add("CREATE TABLE IF NOT EXISTS sync_control (id INTEGER PRIMARY KEY CHECK (id = 1), applying INTEGER NOT NULL)")
        add("INSERT OR IGNORE INTO sync_control (id, applying) VALUES (1, 0)")
        (TABLES + ("track_log" to "song")).forEach { (table, kind) -> addAll(itemTriggers(table, kind)) }
        addAll(groupTriggers("route_points", "routeId", "routes", "route_points"))
        addAll(groupTriggers("location_log", "sessionId", "sessions", "track"))
        addAll(goneTriggers())
    }

    fun statements(): List<String> = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS sync_changes (entity TEXT NOT NULL, uuid TEXT NOT NULL, " +
                "changed_at INTEGER NOT NULL, deleted INTEGER NOT NULL, PRIMARY KEY (entity, uuid))"
        )
        add("CREATE TABLE IF NOT EXISTS sync_control (id INTEGER PRIMARY KEY CHECK (id = 1), applying INTEGER NOT NULL)")
        add("INSERT OR IGNORE INTO sync_control (id, applying) VALUES (1, 0)")

        TABLES.forEach { (table, kind) -> addAll(tableStatements(table, kind)) }

        // A route's points, and a session's GPS points, sync as one item each.
        addAll(groupTriggers("route_points", "routeId", "routes", "route_points"))
        addAll(groupTriggers("location_log", "sessionId", "sessions", "track"))
        add(
            "INSERT OR REPLACE INTO sync_changes SELECT 'route_points', r.uuid, $NOW_MS, 0 FROM routes r " +
                "WHERE EXISTS (SELECT 1 FROM route_points p WHERE p.routeId = r.id)"
        )
        add(
            "INSERT OR REPLACE INTO sync_changes SELECT 'track', s.uuid, $NOW_MS, 0 FROM sessions s " +
                "WHERE EXISTS (SELECT 1 FROM location_log l WHERE l.sessionId = s.id)"
        )
        addAll(goneTriggers())
    }

    // Deleting a route or a session deletes its points with it, on the server too.
    private fun goneTriggers() = listOf(
        "CREATE TRIGGER IF NOT EXISTS sync_routes_points_gone AFTER DELETE ON routes " +
            "WHEN OLD.uuid IS NOT NULL AND $NOT_APPLYING BEGIN " +
            "INSERT OR REPLACE INTO sync_changes VALUES ('route_points', OLD.uuid, $NOW_MS, 1); END",
        "CREATE TRIGGER IF NOT EXISTS sync_sessions_track_gone AFTER DELETE ON sessions " +
            "WHEN OLD.uuid IS NOT NULL AND $NOT_APPLYING BEGIN " +
            "INSERT OR REPLACE INTO sync_changes VALUES ('track', OLD.uuid, $NOW_MS, 1); END",
    )

    private fun tableStatements(table: String, kind: String) = buildList {
        add("ALTER TABLE `$table` ADD COLUMN `uuid` TEXT")
        add("UPDATE `$table` SET uuid = $NEW_UUID WHERE uuid IS NULL")
        add("CREATE UNIQUE INDEX IF NOT EXISTS `index_${table}_uuid` ON `$table` (`uuid`)")
        addAll(itemTriggers(table, kind))
        add("INSERT OR REPLACE INTO sync_changes SELECT '$kind', uuid, $NOW_MS, 0 FROM `$table`")
    }

    private fun itemTriggers(table: String, kind: String) = listOf(
        // A new row gets its uuid here when the code that made it didn't set one.
        "CREATE TRIGGER IF NOT EXISTS sync_${table}_insert AFTER INSERT ON `$table` BEGIN " +
            "UPDATE `$table` SET uuid = $NEW_UUID WHERE rowid = NEW.rowid AND NEW.uuid IS NULL; " +
            "INSERT OR REPLACE INTO sync_changes SELECT '$kind', uuid, $NOW_MS, 0 FROM `$table` " +
            "WHERE rowid = NEW.rowid AND $NOT_APPLYING; END",
        // An update from an entity built without its uuid would clear it; keep the old one.
        "CREATE TRIGGER IF NOT EXISTS sync_${table}_update AFTER UPDATE ON `$table` BEGIN " +
            "UPDATE `$table` SET uuid = OLD.uuid WHERE rowid = NEW.rowid AND NEW.uuid IS NULL; " +
            "INSERT OR REPLACE INTO sync_changes SELECT '$kind', coalesce(NEW.uuid, OLD.uuid), $NOW_MS, 0 " +
            "WHERE coalesce(NEW.uuid, OLD.uuid) IS NOT NULL AND $NOT_APPLYING; END",
        "CREATE TRIGGER IF NOT EXISTS sync_${table}_delete AFTER DELETE ON `$table` " +
            "WHEN OLD.uuid IS NOT NULL AND $NOT_APPLYING BEGIN " +
            "INSERT OR REPLACE INTO sync_changes VALUES ('$kind', OLD.uuid, $NOW_MS, 1); END",
    )

    private fun groupTriggers(table: String, parentColumn: String, parentTable: String, kind: String): List<String> {
        val log = "INSERT OR REPLACE INTO sync_changes SELECT '$kind', p.uuid, $NOW_MS, 0 FROM `$parentTable` p " +
            "WHERE p.id = %s AND p.uuid IS NOT NULL AND $NOT_APPLYING;"
        return listOf(
            "CREATE TRIGGER IF NOT EXISTS sync_${table}_insert AFTER INSERT ON `$table` BEGIN " +
                log.format("NEW.$parentColumn") + " END",
            "CREATE TRIGGER IF NOT EXISTS sync_${table}_update AFTER UPDATE ON `$table` BEGIN " +
                log.format("NEW.$parentColumn") + " END",
            "CREATE TRIGGER IF NOT EXISTS sync_${table}_delete AFTER DELETE ON `$table` BEGIN " +
                log.format("OLD.$parentColumn") + " END",
        )
    }
}
