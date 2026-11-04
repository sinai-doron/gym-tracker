package com.doron.gymtracker.data.templates

import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.LibraryExercise
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TargetDefaults
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The templates in assets/templates, read as the app reads them, against the real catalogue. */
class ProgramTemplateTest {

    private val templates = File("src/main/assets/${ProgramTemplate.DIR}").listFiles { f -> f.extension == "json" }!!
        .map { ProgramTemplate.parse(it.readText()) }
        .sortedBy { it.order }

    private val catalogue = ExerciseCatalogue.parse(File("src/main/assets/${ExerciseCatalogue.ASSET}").readText())

    private fun library(lang: String): Map<String, LibraryExercise> = catalogue.exercises.associate { e ->
        val id = ExerciseCatalogue.builtinId(e.slug)
        id to LibraryExercise(
            id = id,
            name = e.names[lang] ?: e.names.getValue("en"),
            searchNames = e.names.values.toList(),
            preset = e.preset,
            equipment = e.equipment,
            primaryMuscles = e.muscles.primary,
            secondaryMuscles = e.muscles.secondary,
            perSide = e.sides == "per_side",
            isCustom = false,
            defaults = e.defaults.let { TargetDefaults(it.sets, it.repsMin, it.repsMax, it.restS, it.durationS, it.distanceM) },
        )
    }

    private fun build(id: String, lang: String = "en") =
        templates.first { it.id == id }.toNewWorkouts(library(lang), lang) { "row" }

    @Test
    fun `the four templates, in order, each named and summed up in English and Hebrew`() {
        assertEquals(listOf("full_body", "upper_lower", "push_pull_legs", "abc_split"), templates.map { it.id })
        templates.forEach { t ->
            assertEquals(t.id, setOf("en", "he"), t.names.keys)
            assertEquals(t.id, setOf("en", "he"), t.summary.keys)
            t.workouts.forEach { w ->
                assertEquals(t.id, setOf("en", "he"), w.code.keys)
                assertEquals(t.id, setOf("en", "he"), w.names.keys)
                assertTrue(t.id, w.exercises.isNotEmpty())
            }
            listOf("en", "he").forEach { lang ->
                val codes = t.workouts.map { it.code.getValue(lang) }
                assertEquals("${t.id}: codes repeat", codes.size, codes.toSet().size)
            }
        }
    }

    @Test
    fun `every template exercise is in the library, and builds in both languages`() {
        templates.forEach { t ->
            listOf("en", "he").forEach { lang ->
                val workouts = t.toNewWorkouts(library(lang), lang) { "row" }
                assertEquals(t.workouts.size, workouts.size)
                workouts.flatMap { it.exercises }.forEach { (exercise, libraryId) ->
                    assertTrue(exercise.name.isNotBlank())
                    assertTrue(libraryId != null)
                }
            }
        }
    }

    @Test
    fun `targets left out come from the library's defaults`() {
        val squat = build("full_body").first().exercises.first().first as RepExerciseDefinition
        assertEquals("Barbell back squat", squat.name)
        assertEquals(3, squat.targetSets)
        assertEquals(6, squat.targetRepsMin)
        assertEquals(10, squat.targetReps)
        assertEquals(120, squat.restDurationSecs)
    }

    @Test
    fun `A B C keeps the owner's fixed targets, optional exercises and the carry`() {
        val (a, b, c) = build("abc_split", "he")
        assertEquals("חזה וגב", a.name)
        val bench = a.exercises.first().first as RepExerciseDefinition
        assertEquals(4, bench.targetSets)
        assertEquals(6, bench.targetReps)
        assertNull(bench.targetRepsMin)
        assertEquals(180, bench.restDurationSecs)
        assertTrue(a.exercises.last().first is TimedExerciseDefinition)
        assertTrue(b.exercises.single { it.first.isOptional }.first.name.isNotBlank())
        val carry = c.exercises.last().first as DistanceExerciseDefinition
        assertEquals(35, carry.targetDistanceM)
        assertFalse(carry.isOptional)
    }

    @Test
    fun `codes follow the language`() {
        assertEquals(listOf("Push", "Pull", "Legs"), build("push_pull_legs").map { it.code })
        assertEquals(listOf("דחיפה", "משיכה", "רגליים"), build("push_pull_legs", "he").map { it.code })
        assertEquals(listOf("UA", "LA", "UB", "LB"), build("upper_lower", "he").map { it.code })
    }
}
