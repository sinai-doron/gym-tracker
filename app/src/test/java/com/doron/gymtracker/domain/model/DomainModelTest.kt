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
    fun `the seeded plan rotates A, B, C`() {
        assertEquals(listOf("A", "B", "C"), com.doron.gymtracker.data.seed.TrainingPlanData.all.map { it.code })
    }

    @Test
    fun `reps read as a range or a fixed count`() {
        assertEquals("6–10", RepExerciseDefinition("e", "Bench", 3, 120, 10, targetRepsMin = 6).repsLabel)
        assertEquals("8", RepExerciseDefinition("e", "Bench", 3, 120, 8).repsLabel)
        assertEquals("a range of one number is a fixed count", "8", RepExerciseDefinition("e", "Bench", 3, 120, 8, targetRepsMin = 8).repsLabel)
    }

    @Test
    fun `a workout's length is its sets of rest plus work`() {
        // 3 × (120 + 40) + 2 × (60 + 40) = 680 s, about 11 minutes
        val workout = Workout("w", "A", listOf(RepExerciseDefinition("a", "A", 3, 120, 8), TimedExerciseDefinition("b", "B", 2, 60)))
        assertEquals(11, workout.estimatedMinutes())
        assertEquals(0, Workout("e", "E", emptyList()).estimatedMinutes())
    }
}
