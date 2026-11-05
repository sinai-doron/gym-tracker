package com.doron.gymtracker.domain.model

sealed interface ExerciseDefinition {
    val id: String
    /** The name as saved: the library's name in the language it was added in, or the user's own. */
    val name: String
    /**
     * The library exercise's name in each language ("en", "he"), when [name] is one of its names;
     * empty for the user's own name, so a rename is never translated over.
     */
    val names: Map<String, String>
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
    override val names: Map<String, String> = emptyMap(),
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
    override val isPerSide: Boolean = false,
    override val names: Map<String, String> = emptyMap()
) : ExerciseDefinition

data class DistanceExerciseDefinition(
    override val id: String,
    override val name: String,
    override val targetSets: Int,
    override val restDurationSecs: Int,
    val targetDistanceM: Int,
    override val variantB: ExerciseDefinition? = null,
    override val isOptional: Boolean = false,
    override val isPerSide: Boolean = false,
    override val names: Map<String, String> = emptyMap()
) : ExerciseDefinition

/** The name to show in [lang] ("en", "he"): the library's, else the saved one. */
fun ExerciseDefinition.nameIn(lang: String): String = names[lang] ?: name

/** The same exercise planned for [sets] sets. */
fun ExerciseDefinition.withTargetSets(sets: Int): ExerciseDefinition = when (this) {
    is RepExerciseDefinition -> copy(targetSets = sets)
    is TimedExerciseDefinition -> copy(targetSets = sets)
    is DistanceExerciseDefinition -> copy(targetSets = sets)
}
