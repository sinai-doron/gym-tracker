package com.doron.gymtracker.domain.model

import kotlin.math.roundToInt

/** Which screen records a training type's sessions. */
enum class Recorder {
    /** The gym session, with a programme's exercises. */
    EXERCISES,
    /** The run screen: GPS, laps, a route. */
    GPS,
    /** A timer: start, stop, and optional calories and a note. */
    DURATION
}

/**
 * What a session is: strength, a run, Pilates, football… Built-ins have a fixed id and take their
 * name from the app's strings ([name] null); a user's own have the name they typed.
 */
data class TrainingType(
    val id: String,
    val name: String?,
    /** One of TrainingTypeIcons' slugs. */
    val icon: String,
    /** "#RRGGBB". */
    val color: String,
    val recorder: Recorder,
    val healthConnectType: Int?,
    val metValue: Double?,
    val builtin: Boolean,
    val sortOrder: Int,
    val hidden: Boolean,
) {
    /** Calories for [durationS] seconds of it at [bodyWeightKg]: MET × kg × hours. Null without a MET or a weight. */
    fun estimateCalories(durationS: Long, bodyWeightKg: Float?): Int? {
        val met = metValue ?: return null
        val kg = bodyWeightKg ?: return null
        return (met * kg * durationS / 3600.0).roundToInt()
    }
}
