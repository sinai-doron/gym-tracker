package com.doron.gymtracker.domain.model

data class ExerciseSessionSets(
    val sessionId: Long,
    val startedAt: Long,
    val sets: List<SetRecord>
)

/** What the progress chart tracks, chosen by exercise type. */
enum class ProgressMetric { WEIGHT, REPS, DURATION, DISTANCE }

/** The session's best value for the metric (heaviest weight, most reps, ...). */
data class ProgressPoint(
    val sessionId: Long,
    val date: Long,
    val value: Float
)

data class ExerciseProgress(
    val exercise: ExerciseDefinition,
    val metric: ProgressMetric,
    /** Oldest first. */
    val points: List<ProgressPoint>,
    /** Personal record: the highest point (the most recent one on a tie). */
    val best: ProgressPoint?,
    /** Newest first. */
    val sessions: List<ExerciseSessionSets>
)
