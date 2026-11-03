package com.doron.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Version 17: what the programme builder and training types need (phase 2 of the roadmap).
 *
 * - Training types become rows (`activity_types`), starting with the three the app had: Strength,
 *   Run and Walk. Every session names its type; the owner's become Strength, Run or Walk by the
 *   type they already had. `sessions.type` stays, saying which screen records the session.
 * - A session can be logged after the fact: whether it was, and its duration, calories and note.
 * - An exercise's target reps can be a range: `targetRepsMin` up to `targetReps`. Null keeps a
 *   fixed count, so nothing the owner sees changes.
 * - Programmes are archived and workouts hidden instead of deleted, since history points at them.
 *   A programme can name a training type; null is a strength programme.
 * - A user's own library exercises sync, with their names and muscles. The built-ins don't: every
 *   phone has them from the catalogue, under the same ids.
 */
object ActivityTypeMigration {

    const val STRENGTH = "strength"
    const val RUN = "run"
    const val WALK = "walk"

    val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements().forEach(db::execSQL)
        }
    }

    /** The training type a session recorded by [sessionType]'s screen had before version 17. */
    fun forSessionType(sessionType: String): String = when (sessionType) {
        "RUNNING" -> RUN
        "WALKING" -> WALK
        else -> STRENGTH
    }

    /** The built-ins, written with the ids, colours and Health Connect types the app used. */
    fun builtinRows(): List<String> = listOf(
        builtin(STRENGTH, "dumbbell", "#6C5CE7", "EXERCISES", 70, 5.0, 1),
        builtin(RUN, "run", "#00B894", "GPS", 56, 9.8, 2),
        builtin(WALK, "walk", "#00B894", "GPS", 79, 3.5, 3),
    )

    private fun builtin(id: String, icon: String, color: String, recorder: String, hc: Int, met: Double, order: Int) =
        "INSERT OR IGNORE INTO activity_types (id, name, icon, color, recorder, healthConnectType, metValue, builtin, sortOrder) " +
            "VALUES ('$id', NULL, '$icon', '$color', '$recorder', $hc, $met, 1, $order)"

    fun statements() = buildList {
        add(
            "CREATE TABLE IF NOT EXISTS `activity_types` (`id` TEXT NOT NULL, `name` TEXT, `icon` TEXT NOT NULL, " +
                "`color` TEXT NOT NULL, `recorder` TEXT NOT NULL, `healthConnectType` INTEGER, `metValue` REAL, " +
                "`builtin` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `hiddenAt` INTEGER, `uuid` TEXT, PRIMARY KEY(`id`))"
        )
        add("CREATE UNIQUE INDEX IF NOT EXISTS `index_activity_types_uuid` ON `activity_types` (`uuid`)")
        // Before the rows, so they get their uuids and are queued the way the app's own rows are
        addAll(SyncMigration.activityTypeTriggers())
        addAll(builtinRows())

        add("ALTER TABLE `sessions` ADD COLUMN `activityTypeId` TEXT NOT NULL DEFAULT '$STRENGTH'")
        add("ALTER TABLE `sessions` ADD COLUMN `loggedManually` INTEGER NOT NULL DEFAULT 0")
        add("ALTER TABLE `sessions` ADD COLUMN `durationS` INTEGER")
        add("ALTER TABLE `sessions` ADD COLUMN `calories` INTEGER")
        add("ALTER TABLE `sessions` ADD COLUMN `note` TEXT")
        add("CREATE INDEX IF NOT EXISTS `index_sessions_activityTypeId` ON `sessions` (`activityTypeId`)")
        add("UPDATE sessions SET activityTypeId = CASE type WHEN 'RUNNING' THEN '$RUN' WHEN 'WALKING' THEN '$WALK' ELSE '$STRENGTH' END")

        add("ALTER TABLE `exercise_definitions` ADD COLUMN `targetRepsMin` INTEGER")
        add("ALTER TABLE `programs` ADD COLUMN `archivedAt` INTEGER")
        add("ALTER TABLE `programs` ADD COLUMN `activityTypeId` TEXT")
        add("ALTER TABLE `workouts` ADD COLUMN `hiddenAt` INTEGER")

        addAll(SyncMigration.libraryExerciseTriggers())
    }
}
