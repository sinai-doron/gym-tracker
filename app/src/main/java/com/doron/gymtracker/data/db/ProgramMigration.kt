package com.doron.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

/**
 * Version 16: programmes (ADR-0006, step 3 of the migration). What was hard-coded becomes data:
 *
 * - The owner's plan becomes one programme, `program_abc`, with the rules the app had: week 1
 *   starts 2026-10-26, a deload every sixth week, week-B variants on even weeks.
 * - Its workouts belong to it, in the order they rotate (A, B, C). The rotation itself is read
 *   from history from now on, so the last workout stored in prefs isn't carried over: on the
 *   owner's phone it named the last finished workout, which history already says.
 * - History points at the library: each entry gets its plan exercise's built-in.
 *
 * The programme, the workouts and the entries are queued for upload, since their shapes changed.
 */
object ProgramMigration {

    const val PROGRAM_ID = "program_abc"
    private val START: LocalDate = LocalDate.of(2026, 10, 26)

    val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements().forEach(db::execSQL)
        }
    }

    fun statements() = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS `programs` (`id` TEXT NOT NULL, `name` TEXT, `startEpochDay` INTEGER NOT NULL, " +
                "`deloadEveryWeeks` INTEGER, `variantRule` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `uuid` TEXT, PRIMARY KEY(`id`))"
        )
        add("CREATE UNIQUE INDEX IF NOT EXISTS `index_programs_uuid` ON `programs` (`uuid`)")
        // Before the row, so it gets its uuid and is queued the way the app's own rows are
        addAll(SyncMigration.programTriggers())
        add(
            "INSERT INTO programs (id, name, startEpochDay, deloadEveryWeeks, variantRule, isActive) " +
                "VALUES ('$PROGRAM_ID', NULL, ${START.toEpochDay()}, 6, 'ALTERNATE_WEEKLY', 1)"
        )

        add("ALTER TABLE `workouts` ADD COLUMN `programId` TEXT NOT NULL DEFAULT '$PROGRAM_ID'")
        add("ALTER TABLE `workouts` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")
        add("ALTER TABLE `workouts` ADD COLUMN `name` TEXT")
        add("ALTER TABLE `workouts` ADD COLUMN `weekday` INTEGER")
        add("CREATE INDEX IF NOT EXISTS `index_workouts_programId` ON `workouts` (`programId`)")
        add("UPDATE workouts SET sortOrder = CASE label WHEN 'A' THEN 1 WHEN 'B' THEN 2 WHEN 'C' THEN 3 ELSE 4 END")

        add("ALTER TABLE `exercise_entries` ADD COLUMN `libraryExerciseId` TEXT")
        add("CREATE INDEX IF NOT EXISTS `index_exercise_entries_libraryExerciseId` ON `exercise_entries` (`libraryExerciseId`)")
        add(
            "UPDATE exercise_entries SET libraryExerciseId = " +
                "(SELECT d.libraryExerciseId FROM exercise_definitions d WHERE d.id = exercise_entries.exerciseId)"
        )
    }
}
