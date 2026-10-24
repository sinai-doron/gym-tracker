package com.doron.gymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test



class DomainModelTest {

    @Test
    fun `RepExerciseDefinition has targetReps`() {
        val ex = RepExerciseDefinition(
            id = "1", name = "Squat", targetSets = 3, restDurationSecs = 90, targetReps = 8
        )
        assertEquals(8, ex.targetReps)
    }

    @Test
    fun `TimedExerciseDefinition is a distinct ExerciseDefinition type`() {
        val ex: ExerciseDefinition = TimedExerciseDefinition(
            id = "2", name = "Plank", targetSets = 2, restDurationSecs = 60
        )
        assertTrue(ex is TimedExerciseDefinition)
    }

    @Test
    fun `SetRecord when expression is exhaustive`() {
        val set: SetRecord = RepSetRecord(
            id = 1L, entryId = 1L, completedAt = 0L, reps = 8, weightKg = 80f
        )
        val label = when (set) {
            is RepSetRecord -> "rep"
            is TimedSetRecord -> "timed"
            is DistanceSetRecord -> "distance"
        }
        assertEquals("rep", label)
    }

    @Test
    fun `DistanceSetRecord when is exhaustive`() {
        val set: SetRecord = DistanceSetRecord(id = 3L, entryId = 1L, completedAt = 0L, distanceM = 35)
        val label = when (set) {
            is RepSetRecord -> "rep"
            is TimedSetRecord -> "timed"
            is DistanceSetRecord -> "distance"
        }
        assertEquals("distance", label)
    }

    @Test
    fun `TimedSetRecord stores durationSecs`() {
        val set = TimedSetRecord(id = 2L, entryId = 1L, completedAt = 0L, durationSecs = 45)
        assertEquals(45, set.durationSecs)
    }

    @Test
    fun `Workout label enum has A B C`() {
        assertEquals(3, WorkoutLabel.entries.size)
        assertTrue(WorkoutLabel.entries.containsAll(listOf(WorkoutLabel.A, WorkoutLabel.B, WorkoutLabel.C)))
    }
}
