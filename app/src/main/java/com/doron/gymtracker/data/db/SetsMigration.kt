package com.doron.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 15: sets made of parts (ADR-0006, step 2 of the migration). Every `set_records` row
 * becomes one set with one part, keeping its id and uuid, so entries, sync and the server's copy
 * still point at it:
 *
 * - its position in its exercise comes from the order it was done in;
 * - a bodyweight exercise's set gets no load, and so does any set saved with 0 kg (the app saved
 *   bodyweight sets as 0 kg; the crunches' 80 kg was the machine's setting, not a load);
 * - the load is otherwise kept as it was: what it means (per hand for dumbbells, the total for a
 *   barbell) is the library exercise's load convention, not the number;
 * - a farmer's walk keeps its distance; its weight was never saved.
 *
 * Every set is queued for upload, since the server takes them in the new shape.
 */
object SetsMigration {

    val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements().forEach(db::execSQL)
        }
    }

    fun statements() = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS `sets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, " +
                "`seq` INTEGER NOT NULL, `kind` TEXT NOT NULL, `rpe` REAL, `note` TEXT, `completedAt` INTEGER NOT NULL, `uuid` TEXT)"
        )
        add("CREATE UNIQUE INDEX IF NOT EXISTS `index_sets_uuid` ON `sets` (`uuid`)")
        add("CREATE INDEX IF NOT EXISTS `index_sets_entryId` ON `sets` (`entryId`)")
        add(
            "CREATE TABLE IF NOT EXISTS `set_parts` (`setId` INTEGER NOT NULL, `seq` INTEGER NOT NULL, `side` TEXT, " +
                "`reps` INTEGER, `loadKg` REAL, `durationS` INTEGER, `distanceM` INTEGER, `calories` INTEGER, `extra` TEXT, " +
                "PRIMARY KEY(`setId`, `seq`))"
        )
        add("CREATE INDEX IF NOT EXISTS `index_set_parts_setId` ON `set_parts` (`setId`)")

        // No window functions before Android 11's SQLite: a set's position is how many in its
        // exercise came before it, by time and then by id.
        add(
            "INSERT INTO sets (id, entryId, seq, kind, rpe, note, completedAt, uuid) " +
                "SELECT r.id, r.entryId, (SELECT count(*) FROM set_records o WHERE o.entryId = r.entryId AND " +
                "(o.completedAt < r.completedAt OR (o.completedAt = r.completedAt AND o.id < r.id))) + 1, " +
                "'WORKING', NULL, NULL, r.completedAt, r.uuid FROM set_records r"
        )
        add(
            "INSERT INTO set_parts (setId, seq, side, reps, loadKg, durationS, distanceM, calories, extra) " +
                "SELECT r.id, 1, NULL, r.reps, " +
                "CASE WHEN coalesce(d.isBodyweight, 0) = 1 OR r.weightKg = 0 THEN NULL ELSE r.weightKg END, " +
                "r.durationSecs, r.distanceM, NULL, NULL FROM set_records r " +
                "LEFT JOIN exercise_entries e ON e.id = r.entryId LEFT JOIN exercise_definitions d ON d.id = e.exerciseId"
        )
        // Its sync triggers go with it
        add("DROP TABLE set_records")
        addAll(SyncMigration.setTriggers())
        add("INSERT OR REPLACE INTO sync_changes SELECT 'set', uuid, ${SyncMigration.NOW_MS}, 0 FROM sets WHERE uuid IS NOT NULL")
    }
}
