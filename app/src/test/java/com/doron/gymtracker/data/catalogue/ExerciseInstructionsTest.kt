package com.doron.gymtracker.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExerciseInstructionsTest {

    @Test
    fun `steps are keyed by the built-in's id, in each language they have`() {
        val parsed = ExerciseInstructions.parse(
            """{"version": 1, "exercises": [
                {"slug": "barbell-bench-press", "instructions": {"en": ["Lie down"], "he": ["לשכב"]}, "other": 1},
                {"slug": "plank", "instructions": {}}
            ]}"""
        )
        assertEquals(setOf(ExerciseCatalogue.builtinId("barbell-bench-press")), parsed.keys)
        assertEquals(listOf("לשכב"), parsed.getValue(ExerciseCatalogue.builtinId("barbell-bench-press"))["he"])
    }

    @Test
    fun `steps come in the app's language, else in English`() {
        val steps = mapOf("en" to listOf("Lie down"), "he" to listOf("לשכב"))
        assertEquals(listOf("לשכב"), ExerciseInstructions.pick(steps, "he"))
        assertEquals(listOf("Lie down"), ExerciseInstructions.pick(steps, "fr"))
        assertEquals(listOf("Lie down"), ExerciseInstructions.pick(mapOf("en" to listOf("Lie down")), "he"))
        assertEquals(emptyList<String>(), ExerciseInstructions.pick(emptyMap(), "he"))
    }

    @Test
    fun `the shipped catalogue has Hebrew steps for every exercise with English ones, one for one`() {
        val asset = File("src/main/assets/${ExerciseCatalogue.ASSET}")
        val parsed = ExerciseInstructions.parse(asset.readText())
        assertTrue("instructions for most built-ins", parsed.size > 150)
        parsed.forEach { (id, byLanguage) ->
            assertEquals("Hebrew and English steps for $id", byLanguage["en"]?.size, byLanguage["he"]?.size)
        }
    }
}
