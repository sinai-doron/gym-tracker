package com.doron.gymtracker.data.catalogue

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Who the exercise catalogue's content comes from, read from the same asset as ExerciseCatalogue:
 * the catalogue's licence, and each exercise's wger source with its authors and licences. The
 * Open-source licences screen shows it.
 */
@Serializable
data class CatalogueCredits(val license: String, val exercises: List<CreditedExercise>) {

    /** The exercises that take something from another source; the rest are wholly ours. */
    val credited: List<CreditedExercise> get() = exercises.filter { it.source != null }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): CatalogueCredits = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class CreditedExercise(val slug: String, val names: Map<String, String>, val source: ExerciseSource? = null)

@Serializable
data class ExerciseSource(
    val name: String,
    val url: String,
    val exerciseLicense: String? = null,
    val exerciseAuthors: List<String> = emptyList(),
    val textLicense: String? = null,
    val textAuthors: List<String> = emptyList(),
) {
    /** Everyone who wrote the exercise or its instructions, once each. */
    val authors: List<String> get() = (exerciseAuthors + textAuthors).distinct()

    val licenses: List<String> get() = listOfNotNull(exerciseLicense, textLicense).distinct()
}
