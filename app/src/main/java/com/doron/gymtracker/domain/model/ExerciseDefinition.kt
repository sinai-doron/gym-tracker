package com.doron.gymtracker.domain.model

sealed interface ExerciseDefinition {
    val id: String
    val name: String
    val targetSets: Int
    val restDurationSecs: Int
    val variantB: ExerciseDefinition?
    val isOptional: Boolean
    val isPerSide: Boolean
}

data class RepExerciseDefinition(
    override val id: String,
    override val name: String,
    override val targetSets: Int,
    override val restDurationSecs: Int,
    val targetReps: Int,
    override val variantB: ExerciseDefinition? = null,
    override val isOptional: Boolean = false,
    override val isPerSide: Boolean = false
) : ExerciseDefinition

data class TimedExerciseDefinition(
    override val id: String,
    override val name: String,
    override val targetSets: Int,
    override val restDurationSecs: Int,
    override val variantB: ExerciseDefinition? = null,
    override val isOptional: Boolean = false,
    override val isPerSide: Boolean = false
) : ExerciseDefinition

data class DistanceExerciseDefinition(
    override val id: String,
    override val name: String,
    override val targetSets: Int,
    override val restDurationSecs: Int,
    val targetDistanceM: Int,
    override val variantB: ExerciseDefinition? = null,
    override val isOptional: Boolean = false,
    override val isPerSide: Boolean = false
) : ExerciseDefinition
