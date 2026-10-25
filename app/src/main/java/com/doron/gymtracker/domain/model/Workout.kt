package com.doron.gymtracker.domain.model

enum class WorkoutLabel(val displayName: String) {
    A("חזה וגב"),
    B("רגליים"),
    C("כתפיים וידיים")
}

data class Workout(
    val id: String,
    val label: WorkoutLabel,
    val exercises: List<ExerciseDefinition>
)
