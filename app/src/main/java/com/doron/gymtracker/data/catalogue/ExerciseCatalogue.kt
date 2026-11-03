package com.doron.gymtracker.data.catalogue

import com.doron.gymtracker.data.db.entities.ExerciseEntity
import com.doron.gymtracker.data.db.entities.ExerciseMuscleEntity
import com.doron.gymtracker.data.db.entities.ExerciseNameEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * The built-in exercises the app ships (assets/exercises/exercises.json, built by
 * catalogue/build_catalogue.py). Only what the library table holds is read here; instructions and
 * their credits stay in the file.
 */
@Serializable
data class ExerciseCatalogue(val version: Int, val exercises: List<CatalogueExercise>) {

    /** The owner's old plan ids (d1_e1…) and the built-in each one is. */
    val legacyIds: Map<String, String> get() =
        exercises.flatMap { e -> e.legacy.map { it to builtinId(e.slug) } }.toMap()

    fun toEntities(): Triple<List<ExerciseEntity>, List<ExerciseNameEntity>, List<ExerciseMuscleEntity>> =
        Triple(
            exercises.map { e ->
                ExerciseEntity(
                    id = builtinId(e.slug), slug = e.slug, source = BUILTIN, preset = e.preset, load = e.load,
                    sides = e.sides, equipment = e.equipment, mechanics = e.mechanics,
                    weightStepKg = e.weightStepKg, catalogueVersion = version
                )
            },
            exercises.flatMap { e ->
                val id = builtinId(e.slug)
                e.names.map { (lang, name) -> ExerciseNameEntity(id, lang, name, isAlias = false) } +
                    e.aliases.flatMap { (lang, names) -> names.map { ExerciseNameEntity(id, lang, it, isAlias = true) } }
            }.distinctBy { Triple(it.exerciseId, it.lang, it.name) },
            exercises.flatMap { e ->
                val id = builtinId(e.slug)
                e.muscles.primary.map { ExerciseMuscleEntity(id, it, isPrimary = true) } +
                    e.muscles.secondary.map { ExerciseMuscleEntity(id, it, isPrimary = false) }
            }
        )

    companion object {
        const val ASSET = "exercises/exercises.json"
        const val BUILTIN = "BUILTIN"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): ExerciseCatalogue = json.decodeFromString(serializer(), text)

        /**
         * A built-in exercise's id, derived from its slug: the same on every phone and on the server,
         * so history can name a built-in without uploading it. Never change this: synced history and
         * the v14 migration hold ids made by it.
         */
        fun builtinId(slug: String): String =
            UUID.nameUUIDFromBytes("gym-tracker/exercise/$slug".toByteArray(Charsets.UTF_8)).toString()
    }
}

@Serializable
data class CatalogueExercise(
    val slug: String,
    val names: Map<String, String>,
    val aliases: Map<String, List<String>> = emptyMap(),
    val preset: String,
    val load: String,
    val sides: String,
    val equipment: String,
    val mechanics: String? = null,
    val muscles: CatalogueMuscles,
    val weightStepKg: Double? = null,
    val legacy: List<String> = emptyList(),
    val defaults: CatalogueDefaults = CatalogueDefaults(),
)

/** What an exercise starts with when added to a workout. */
@Serializable
data class CatalogueDefaults(
    val sets: Int = 3,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restS: Int = 90,
    val durationS: Int? = null,
    val distanceM: Int? = null,
)

@Serializable
data class CatalogueMuscles(val primary: List<String>, val secondary: List<String> = emptyList())
