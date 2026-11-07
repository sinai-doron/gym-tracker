package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressMetric
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetExerciseProgressUseCaseTest {

    private lateinit var sessionRepo: SessionRepository
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var useCase: GetExerciseProgressUseCase

    @Before
    fun setup() {
        sessionRepo = mockk()
        workoutRepo = mockk()
        coEvery { workoutRepo.getAll() } returns TrainingPlanData.all
        coEvery { workoutRepo.getExercise(any()) } returns null
        useCase = GetExerciseProgressUseCase(sessionRepo, workoutRepo)
    }

    @Test
    fun `an exercise no longer in the active plan is still found, as opened from History`() = runTest {
        coEvery { workoutRepo.getExercise("old") } returns RepExerciseDefinition("old", "Old press", 3, 90, 8)
        coEvery { sessionRepo.getExerciseHistory("old") } returns listOf(
            ExerciseSessionSets(1L, 100L, listOf(rep(8, 30f)))
        )
        val progress = useCase("old")!!
        assertEquals("Old press", progress.exercise.name)
        assertEquals(listOf(30f), progress.points.map { it.value })
    }

    private fun rep(reps: Int, kg: Float) = RepSetRecord(0L, 0L, 0L, reps, kg)

    @Test
    fun `weighted exercise tracks the heaviest set per session`() = runTest {
        coEvery { sessionRepo.getExerciseHistory("d1_e1") } returns listOf(
            ExerciseSessionSets(1L, 100L, listOf(rep(10, 40f), rep(8, 45f))),
            ExerciseSessionSets(2L, 200L, listOf(rep(10, 50f)))
        )
        val progress = useCase("d1_e1")!!
        assertEquals(ProgressMetric.WEIGHT, progress.metric)
        assertEquals(listOf(45f, 50f), progress.points.map { it.value })
        assertEquals(2L, progress.best?.sessionId)
        assertEquals(listOf(2L, 1L), progress.sessions.map { it.sessionId })
    }

    @Test
    fun `bodyweight exercise tracks reps`() = runTest {
        coEvery { sessionRepo.getExerciseHistory("d2_e8a") } returns listOf(
            ExerciseSessionSets(1L, 100L, listOf(rep(12, 0f), rep(15, 0f)))
        )
        val progress = useCase("d2_e8a")!!
        assertEquals(ProgressMetric.REPS, progress.metric)
        assertEquals(15f, progress.best?.value)
    }

    @Test
    fun `personal record tie goes to the most recent session`() = runTest {
        coEvery { sessionRepo.getExerciseHistory("d1_e1") } returns listOf(
            ExerciseSessionSets(1L, 100L, listOf(rep(10, 50f))),
            ExerciseSessionSets(2L, 200L, listOf(rep(10, 50f)))
        )
        assertEquals(2L, useCase("d1_e1")!!.best?.sessionId)
    }

    @Test
    fun `week-B variants are found`() = runTest {
        coEvery { sessionRepo.getExerciseHistory("d1_e3b") } returns emptyList()
        val progress = useCase("d1_e3b")!!
        assertEquals("d1_e3b", progress.exercise.id)
        assertNull(progress.best)
    }

    @Test
    fun `unknown exercise returns null`() = runTest {
        assertNull(useCase("nope"))
    }
}
