package com.doron.gymtracker.domain.model

data class ExercisePreview(
    val exercise: ExerciseDefinition,
    val lastWeightKg: Float?
)

data class WorkoutPreview(
    val workout: Workout,
    val exercises: List<ExercisePreview>,
    val lastDoneAt: Long?,
    val typicalMinutes: Int?
)
