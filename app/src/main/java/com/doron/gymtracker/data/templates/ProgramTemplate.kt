package com.doron.gymtracker.data.templates

import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.repository.NewWorkout
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.LibraryExercise
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TargetDefaults
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * A programme to start from (assets/templates/<id>.json): its workouts, each a list of library
 * exercises by slug. Targets left out of a template come from the library's defaults. Installing
 * one copies it into ordinary rows; nothing points back at the template.
 */
@Serializable
data class ProgramTemplate(
    val id: String,
    /** Where it is listed, first to last. */
    val order: Int,
    /** The few letters on its card: FB, UL, PPL. */
    val badge: String,
    val names: Map<String, String>,
    val summary: Map<String, String>,
    val workouts: List<TemplateWorkout>,
) {
    fun name(lang: String): String = names[lang] ?: names.getValue(EN)

    fun summary(lang: String): String = summary[lang] ?: summary.getValue(EN)

    /**
     * The template's workouts as rows to write, named in [lang]. [library] holds the library's
     * exercises by id; [newId] makes each exercise row's id.
     */
    fun toNewWorkouts(library: Map<String, LibraryExercise>, lang: String, newId: () -> String): List<NewWorkout> =
        workouts.map { w ->
            NewWorkout(
                code = w.code[lang] ?: w.code.getValue(EN),
                name = w.names[lang] ?: w.names.getValue(EN),
                exercises = w.exercises.map { e ->
                    val id = ExerciseCatalogue.builtinId(e.exercise)
                    val exercise = checkNotNull(library[id]) { "template $this: no exercise ${e.exercise} in the library" }
                    e.applyTo(exercise).toDefinition(newId()).optional(e.optional) to id
                }
            )
        }

    override fun toString(): String = id

    companion object {
        const val DIR = "templates"
        private const val EN = "en"
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): ProgramTemplate = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class TemplateWorkout(
    /** The code on the workout's chip, per language: A, UA, Push. */
    val code: Map<String, String>,
    val names: Map<String, String>,
    val exercises: List<TemplateExercise>,
)

/** A library exercise by slug, with any targets that differ from its defaults. */
@Serializable
data class TemplateExercise(
    val exercise: String,
    val sets: Int? = null,
    val repsMin: Int? = null,
    val repsMax: Int? = null,
    val restS: Int? = null,
    val durationS: Int? = null,
    val distanceM: Int? = null,
    val optional: Boolean = false,
) {
    fun applyTo(library: LibraryExercise): LibraryExercise = library.copy(
        defaults = library.defaults.let { d ->
            TargetDefaults(
                sets = sets ?: d.sets,
                repsMin = repsMin ?: d.repsMin,
                repsMax = repsMax ?: d.repsMax,
                restS = restS ?: d.restS,
                durationS = durationS ?: d.durationS,
                distanceM = distanceM ?: d.distanceM,
            )
        }
    )
}

private fun ExerciseDefinition.optional(optional: Boolean): ExerciseDefinition = if (!optional) this else when (this) {
    is RepExerciseDefinition -> copy(isOptional = true)
    is TimedExerciseDefinition -> copy(isOptional = true)
    is DistanceExerciseDefinition -> copy(isOptional = true)
}
