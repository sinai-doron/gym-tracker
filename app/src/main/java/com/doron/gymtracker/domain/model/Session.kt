package com.doron.gymtracker.domain.model

data class Session(
    val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val type: String = "GYM",
    val distanceMeters: Double? = null,
    /** Its training type's id: strength, run, walk, pilates… */
    val activityTypeId: String = "strength",
    /** Entered afterwards rather than recorded live. */
    val loggedManually: Boolean = false,
    /** For a timed or logged session; a recorded one runs from [startedAt] to [finishedAt]. */
    val durationS: Int? = null,
    val calories: Int? = null,
    val note: String? = null
)
