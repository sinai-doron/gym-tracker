package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressMetric
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
    private lateinit var useCase: GetExerciseProgressUseCase

    @Before
    fun setup() {
        sessionRepo = mockk()
        val workoutRepo = mockk<WorkoutRepository>()
        coEvery { workoutRepo.getAll() } returns TrainingPlanData.all
        useCase = GetExerciseProgressUseCase(sessionRepo, workoutRepo)
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
