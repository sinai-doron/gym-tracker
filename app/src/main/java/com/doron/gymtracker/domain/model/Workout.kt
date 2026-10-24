package com.doron.gymtracker.domain.model

enum class WorkoutLabel { A, B, C }

data class Workout(
    val id: String,
    val label: WorkoutLabel,
    val exercises: List<ExerciseDefinition>
)
