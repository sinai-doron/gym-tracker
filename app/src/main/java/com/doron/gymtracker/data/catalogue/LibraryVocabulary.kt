package com.doron.gymtracker.data.catalogue

import androidx.annotation.StringRes
import com.doron.gymtracker.R

/**
 * The words the exercise library is written in, from catalogue/vocabulary.json: muscles in body
 * order, equipment with its weight step, and what a set records (presets). Labels are string
 * resources; LibraryVocabularyTest checks this file against the vocabulary.
 */
object LibraryVocabulary {

    /** The 18 muscles, head to toe. */
    val MUSCLES: List<String> = listOf(
        "chest",
        "front_delts",
        "side_delts",
        "rear_delts",
        "biceps",
        "triceps",
        "forearms",
        "abs",
        "obliques",
        "lats",
        "traps",
        "upper_back",
        "lower_back",
        "glutes",
        "quads",
        "hamstrings",
        "adductors",
        "calves",
    )

    /** Equipment -> the step its weight goes up by, in kg; null for none. */
    val EQUIPMENT_STEP_KG: Map<String, Double?> = linkedMapOf(
        "barbell" to 2.5,
        "ez_bar" to 2.5,
        "dumbbell" to 2.0,
        "kettlebell" to 4.0,
        "cable" to 2.5,
        "machine" to 5.0,
        "smith_machine" to 2.5,
        "bodyweight" to null,
        "band" to null,
        "other" to null,
    )

    val PRESETS: List<String> = listOf("reps_load", "reps", "reps_assisted", "time", "distance_load", "distance_time", "calories_time")

    @StringRes
    fun muscleLabel(slug: String): Int? = when (slug) {
        "chest" -> R.string.muscle_chest
        "front_delts" -> R.string.muscle_front_delts
        "side_delts" -> R.string.muscle_side_delts
        "rear_delts" -> R.string.muscle_rear_delts
        "biceps" -> R.string.muscle_biceps
        "triceps" -> R.string.muscle_triceps
        "forearms" -> R.string.muscle_forearms
        "abs" -> R.string.muscle_abs
        "obliques" -> R.string.muscle_obliques
        "lats" -> R.string.muscle_lats
        "traps" -> R.string.muscle_traps
        "upper_back" -> R.string.muscle_upper_back
        "lower_back" -> R.string.muscle_lower_back
        "glutes" -> R.string.muscle_glutes
        "quads" -> R.string.muscle_quads
        "hamstrings" -> R.string.muscle_hamstrings
        "adductors" -> R.string.muscle_adductors
        "calves" -> R.string.muscle_calves
        else -> null
    }

    @StringRes
    fun equipmentLabel(slug: String): Int? = when (slug) {
        "barbell" -> R.string.equipment_barbell
        "ez_bar" -> R.string.equipment_ez_bar
        "dumbbell" -> R.string.equipment_dumbbell
        "kettlebell" -> R.string.equipment_kettlebell
        "cable" -> R.string.equipment_cable
        "machine" -> R.string.equipment_machine
        "smith_machine" -> R.string.equipment_smith_machine
        "bodyweight" -> R.string.equipment_bodyweight
        "band" -> R.string.equipment_band
        "other" -> R.string.equipment_other
        else -> null
    }

    @StringRes
    fun presetLabel(slug: String): Int? = when (slug) {
        "reps_load" -> R.string.preset_reps_load
        "reps" -> R.string.preset_reps
        "reps_assisted" -> R.string.preset_reps_assisted
        "time" -> R.string.preset_time
        "distance_load" -> R.string.preset_distance_load
        "distance_time" -> R.string.preset_distance_time
        "calories_time" -> R.string.preset_calories_time
        else -> null
    }
}
