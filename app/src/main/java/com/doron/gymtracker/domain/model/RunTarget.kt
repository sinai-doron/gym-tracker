package com.doron.gymtracker.domain.model

enum class RunTargetType { NONE, DISTANCE, DURATION, ROUTE }

/** What a run aims for. Only the value matching [type] matters; the others are kept as last used. */
data class RunTarget(
    val type: RunTargetType = RunTargetType.NONE,
    val distanceKm: Float = DEFAULT_DISTANCE_KM,
    val durationMin: Int = DEFAULT_DURATION_MIN
) {
    /** How far along the target is (1 = reached; can pass 1), or null when there's nothing to measure. */
    fun progress(distanceMeters: Double, elapsedSecs: Long): Float? = when (type) {
        RunTargetType.DISTANCE -> (distanceMeters / 1000.0 / distanceKm).toFloat()
        RunTargetType.DURATION -> elapsedSecs / 60f / durationMin
        RunTargetType.NONE, RunTargetType.ROUTE -> null
    }

    fun isReached(distanceMeters: Double, elapsedSecs: Long): Boolean =
        (progress(distanceMeters, elapsedSecs) ?: 0f) >= 1f

    companion object {
        const val DEFAULT_DISTANCE_KM = 5f
        const val DEFAULT_DURATION_MIN = 30
        const val DISTANCE_STEP_KM = 0.5f
        const val DURATION_STEP_MIN = 5
    }
}
