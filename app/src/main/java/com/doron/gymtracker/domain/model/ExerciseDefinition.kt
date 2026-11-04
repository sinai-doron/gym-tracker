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
    /** The reps to reach; the top of the range when [targetRepsMin] is set. */
    val targetReps: Int,
    override val variantB: ExerciseDefinition? = null,
    override val isOptional: Boolean = false,
    override val isPerSide: Boolean = false,
    /** Bodyweight / free-form: logged by reps only, no weight. */
    val isBodyweight: Boolean = false,
    /** The bottom of a rep range (6 in 6–10); null for a fixed count. */
    val targetRepsMin: Int? = null
) : ExerciseDefinition {
    /** "6–10" for a range, "8" for a fixed count. */
    val repsLabel: String
        get() = targetRepsMin?.takeIf { it < targetReps }?.let { "$it–$targetReps" } ?: "$targetReps"
}

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
