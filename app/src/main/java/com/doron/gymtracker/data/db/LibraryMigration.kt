package com.doron.gymtracker.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue

/**
 * Version 14: the exercise library (ADR-0006, step 1 of the migration). Adds the library's tables
 * and links each of the plan's exercises to the built-in it is. History is untouched: sets, entries
 * and sessions stay as they were, and nothing synced changes shape.
 *
 * The built-ins themselves are written by ExerciseLibrary when the app starts, from the catalogue
 * in the app's assets; until then a link points at an id the library doesn't hold yet.
 */
object LibraryMigration {

    /**
     * The owner's plan, id by id, and the built-in each exercise is. Written out here, not read from
     * the catalogue, so that a later catalogue can't change what this migration did.
     */
    val PLAN_TO_BUILTIN = mapOf(
        "d1_e1" to "barbell-bench-press",
        "d1_e2" to "wide-grip-lat-pulldown",
        "d1_e3a" to "incline-dumbbell-press",
        "d1_e3b" to "incline-barbell-bench-press",
        "d1_e4a" to "chest-supported-barbell-row",
        "d1_e4b" to "one-arm-dumbbell-row",
        "d1_e5a" to "machine-chest-fly",
        "d1_e5b" to "low-to-high-cable-fly",
        "d1_e6a" to "seated-cable-row-v-handle",
        "d1_e6b" to "seated-cable-row-straight-bar",
        "d1_e7" to "plank",
        "d2_e1" to "barbell-back-squat",
        "d2_e2a" to "barbell-romanian-deadlift",
        "d2_e2b" to "dumbbell-romanian-deadlift",
        "d2_e3a" to "bulgarian-split-squat",
        "d2_e3b" to "dumbbell-walking-lunge",
        "d2_e4" to "leg-extension",
        "d2_e5" to "machine-leg-curl",
        "d2_e6" to "hip-adduction-machine",
        "d2_e7" to "smith-machine-calf-raise",
        "d2_e8a" to "crunch",
        "d2_e8b" to "hanging-knee-raise",
        "d3_e1" to "seated-dumbbell-shoulder-press",
        "d3_e2a" to "dumbbell-lateral-raise",
        "d3_e2b" to "cable-lateral-raise",
        "d3_e3a" to "reverse-machine-fly",
        "d3_e3b" to "cable-face-pull",
        "d3_e4a" to "barbell-curl",
        "d3_e4b" to "dumbbell-hammer-curl",
        "d3_e5a" to "triceps-pushdown-bar",
        "d3_e5b" to "triceps-pushdown-rope",
        "d3_e6" to "cable-curl",
        "d3_e7" to "overhead-dumbbell-triceps-extension",
        "d3_e8" to "farmers-walk",
    )

    val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements().forEach(db::execSQL)
            // The link isn't synced, so the plan isn't queued for upload again: written as a download is.
            db.execSQL("UPDATE sync_control SET applying = 1 WHERE id = 1")
            PLAN_TO_BUILTIN.forEach { (planId, slug) ->
                db.execSQL(
                    "UPDATE exercise_definitions SET libraryExerciseId = ? WHERE id = ?",
                    arrayOf<Any?>(ExerciseCatalogue.builtinId(slug), planId)
                )
            }
            db.execSQL("UPDATE sync_control SET applying = 0 WHERE id = 1")
        }
    }

    /** The tables, as Room creates them from the entities (app/schemas/…/14.json). */
    fun statements() = listOf(
        "ALTER TABLE `exercise_definitions` ADD COLUMN `libraryExerciseId` TEXT",
        "CREATE INDEX IF NOT EXISTS `index_exercise_definitions_libraryExerciseId` ON `exercise_definitions` (`libraryExerciseId`)",
        "CREATE TABLE IF NOT EXISTS `exercises` (`id` TEXT NOT NULL, `slug` TEXT, `source` TEXT NOT NULL, " +
            "`preset` TEXT NOT NULL, `load` TEXT NOT NULL, `loadUnit` TEXT, `sides` TEXT NOT NULL, `equipment` TEXT NOT NULL, " +
            "`mechanics` TEXT, `weightStepKg` REAL, `catalogueVersion` INTEGER, `archivedAt` INTEGER, PRIMARY KEY(`id`))",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_exercises_slug` ON `exercises` (`slug`)",
        "CREATE TABLE IF NOT EXISTS `exercise_names` (`exerciseId` TEXT NOT NULL, `lang` TEXT NOT NULL, " +
            "`name` TEXT NOT NULL, `isAlias` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`, `lang`, `name`))",
        "CREATE INDEX IF NOT EXISTS `index_exercise_names_name` ON `exercise_names` (`name`)",
        "CREATE TABLE IF NOT EXISTS `exercise_muscles` (`exerciseId` TEXT NOT NULL, `muscle` TEXT NOT NULL, " +
            "`isPrimary` INTEGER NOT NULL, PRIMARY KEY(`exerciseId`, `muscle`))",
        "CREATE INDEX IF NOT EXISTS `index_exercise_muscles_muscle` ON `exercise_muscles` (`muscle`)",
    )
}
