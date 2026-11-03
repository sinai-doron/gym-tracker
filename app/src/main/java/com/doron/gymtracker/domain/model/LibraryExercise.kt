package com.doron.gymtracker.domain.model

/** What a library exercise starts with when added to a workout. */
data class TargetDefaults(
    val sets: Int = 3,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restS: Int = 90,
    val durationS: Int? = null,
    val distanceM: Int? = null,
)

/** An exercise in the library, as the picker shows it: named in the app's language. */
data class LibraryExercise(
    val id: String,
    /** In the app's language, else English, else any. */
    val name: String,
    /** Every name and alias in every language, for search: "bench" and "בנץ'" both find the bench press. */
    val searchNames: List<String>,
    /** What a set records: reps_load, reps, reps_assisted, time, distance_load, distance_time, calories_time. */
    val preset: String,
    val equipment: String,
    val primaryMuscles: List<String>,
    val secondaryMuscles: List<String>,
    val perSide: Boolean,
    val isCustom: Boolean,
    val defaults: TargetDefaults,
) {
    /**
     * This exercise as a new row of a workout, with its default targets. Reps with a weight are
     * logged with one; bodyweight reps without; time and calories by time; distances by distance.
     */
    fun toDefinition(id: String): ExerciseDefinition = when (preset) {
        "time", "calories_time" -> TimedExerciseDefinition(id, name, defaults.sets, defaults.restS, isPerSide = perSide)
        "distance_load", "distance_time" -> DistanceExerciseDefinition(
            id, name, defaults.sets, defaults.restS, defaults.distanceM ?: DEFAULT_DISTANCE_M, isPerSide = perSide
        )
        else -> {
            val max = defaults.repsMax ?: DEFAULT_REPS
            RepExerciseDefinition(
                id, name, defaults.sets, defaults.restS, max,
                isPerSide = perSide, isBodyweight = preset == "reps", targetRepsMin = defaults.repsMin?.takeIf { it < max }
            )
        }
    }

    private companion object {
        const val DEFAULT_REPS = 10
        const val DEFAULT_DISTANCE_M = 1000
    }
}

/** Search and similar-name matching over the library, kept pure so it can be tested. */
object LibrarySearch {

    /**
     * Exercises matching [text] in any name or alias, working [muscles] (any of them, primary or
     * secondary) and using any of [equipment]; empty filters match all. Primary-muscle matches and
     * [recent] ones come first, then by name.
     */
    fun filter(
        all: List<LibraryExercise>,
        text: String,
        muscles: Set<String>,
        equipment: Set<String>,
        recent: List<String> = emptyList(),
    ): List<LibraryExercise> {
        val query = normalize(text)
        return all.filter { e ->
            (query.isEmpty() || e.searchNames.any { normalize(it).contains(query) }) &&
                (muscles.isEmpty() || (e.primaryMuscles + e.secondaryMuscles).any { it in muscles }) &&
                (equipment.isEmpty() || e.equipment in equipment)
        }.sortedWith(
            compareBy<LibraryExercise>(
                { if (muscles.isNotEmpty() && it.primaryMuscles.none { m -> m in muscles }) 1 else 0 },
                { recent.indexOf(it.id).let { i -> if (i < 0) Int.MAX_VALUE else i } },
                { it.name }
            )
        )
    }

    /** Up to [limit] exercises whose names share a word of 3 letters or more with [name], most shared first. */
    fun similar(all: List<LibraryExercise>, name: String, limit: Int = 2): List<LibraryExercise> {
        val words = words(name)
        if (words.isEmpty()) return emptyList()
        return all.map { e -> e to e.searchNames.maxOf { n -> words(n).count { it in words } } }
            .filter { (_, shared) -> shared > 0 }
            .sortedWith(compareByDescending<Pair<LibraryExercise, Int>> { it.second }.thenBy { it.first.name })
            .take(limit)
            .map { it.first }
    }

    private fun words(text: String): Set<String> =
        normalize(text).split(' ', '-', '(', ')', ',', '/').filter { it.length >= 3 }.toSet()

    private fun normalize(text: String): String = text.trim().lowercase().replace('’', '\'')
}
