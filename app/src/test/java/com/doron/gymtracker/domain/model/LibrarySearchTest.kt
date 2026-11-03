package com.doron.gymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchTest {

    private fun ex(
        id: String, name: String, preset: String = "reps_load", equipment: String = "barbell",
        primary: List<String> = listOf("chest"), secondary: List<String> = emptyList(), aliases: List<String> = emptyList(),
        defaults: TargetDefaults = TargetDefaults(3, 6, 10, 120)
    ) = LibraryExercise(id, name, listOf(name) + aliases, preset, equipment, primary, secondary, false, false, defaults)

    private val bench = ex("bench", "Barbell bench press", aliases = listOf("flat bench", "בנץ'"))
    private val dbBench = ex("db", "Dumbbell bench press", equipment = "dumbbell", secondary = listOf("triceps"))
    private val pushdown = ex("push", "Cable triceps pushdown", equipment = "cable", primary = listOf("triceps"))
    private val row = ex("row", "Seated cable row", equipment = "cable", primary = listOf("upper_back"), secondary = listOf("lats"))
    private val all = listOf(row, pushdown, dbBench, bench)

    @Test
    fun `search finds any name or alias in any language`() {
        assertEquals(listOf("bench"), LibrarySearch.filter(all, "בנץ", emptySet(), emptySet()).map { it.id })
        assertEquals(listOf("bench", "db"), LibrarySearch.filter(all, "BENCH", emptySet(), emptySet()).map { it.id })
    }

    @Test
    fun `filters combine, and primary muscles come before secondary ones`() {
        assertEquals(listOf("push", "db"), LibrarySearch.filter(all, "", setOf("triceps"), emptySet()).map { it.id })
        assertEquals(listOf("push"), LibrarySearch.filter(all, "", setOf("triceps"), setOf("cable")).map { it.id })
    }

    @Test
    fun `recently done ones lead an unfiltered list, the rest by name`() {
        assertEquals(
            listOf("row", "bench", "push", "db"),
            LibrarySearch.filter(all, "", emptySet(), emptySet(), recent = listOf("row")).map { it.id }
        )
    }

    @Test
    fun `similar names share a word of three letters or more`() {
        assertEquals(listOf("bench", "db"), LibrarySearch.similar(all, "Incline bench press").map { it.id })
        assertTrue(LibrarySearch.similar(all, "Landmine squat").isEmpty())
        assertTrue("short words don't count", LibrarySearch.similar(all, "a an to").isEmpty())
    }

    @Test
    fun `a library exercise becomes a workout row with its defaults`() {
        val reps = bench.toDefinition("x") as RepExerciseDefinition
        assertEquals(listOf(3, 120, 10, 6), listOf(reps.targetSets, reps.restDurationSecs, reps.targetReps, reps.targetRepsMin))
        assertEquals(false, reps.isBodyweight)

        val pushups = ex("pu", "Push-up", preset = "reps", equipment = "bodyweight", defaults = TargetDefaults(3, 10, 15, 60))
        assertTrue((pushups.toDefinition("x") as RepExerciseDefinition).isBodyweight)

        val plank = ex("pl", "Plank", preset = "time", defaults = TargetDefaults(3, restS = 60, durationS = 45))
        assertTrue(plank.toDefinition("x") is TimedExerciseDefinition)

        val carry = ex("fw", "Farmer's walk", preset = "distance_load", defaults = TargetDefaults(3, restS = 90, distanceM = 30))
        assertEquals(30, (carry.toDefinition("x") as DistanceExerciseDefinition).targetDistanceM)
    }
}
