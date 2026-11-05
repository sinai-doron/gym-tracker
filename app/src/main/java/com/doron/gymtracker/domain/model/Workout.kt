package com.doron.gymtracker.domain.model

import java.time.DayOfWeek

data class Workout(
    val id: String,
    /** The short code on the workout's chip: A, B, Push… */
    val code: String,
    val exercises: List<ExerciseDefinition>,
    /** The name the user gave it; null shows the built-in name of the seeded plan's workouts. */
    val name: String? = null,
    /** Its place in the programme's rotation. */
    val sortOrder: Int = 0,
    /** The day of the week it is done on, when its programme follows the week; null for none. */
    val weekday: DayOfWeek? = null
) {
    /**
     * A rough length, for a new user to notice a workout growing too long: each set takes its rest
     * plus [SET_SECONDS] of work. History's typical length (WorkoutPreview) replaces it once done.
     */
    fun estimatedMinutes(): Int =
        ((exercises.sumOf { it.targetSets * (it.restDurationSecs + SET_SECONDS) } + 30) / 60)

    private companion object {
        const val SET_SECONDS = 40
    }
}
