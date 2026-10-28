package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetLastWeightUseCaseTest {

    private lateinit var repo: SessionRepository
    private lateinit var useCase: GetLastWeightUseCase

    @Before
    fun setup() {
        repo = mockk()
        useCase = GetLastWeightUseCase(repo)
    }

    @Test
    fun `returns null when no completed sessions`() = runTest {
        coEvery { repo.getCompletedSince(0) } returns emptyList()
        assertNull(useCase("d1_e1"))
    }

    @Test
    fun `returns max weight from last session for the exercise`() = runTest {
        val session = Session(1L, "workout_a", 0L, 1000L)
        coEvery { repo.getCompletedSince(0) } returns listOf(session)
        coEvery { repo.getEntriesForSession(1L) } returns listOf(
            ExerciseEntry(1L, 1L, "d1_e1", listOf(
                RepSetRecord(1L, 1L, 0L, reps = 6, weightKg = 60f),
                RepSetRecord(2L, 1L, 0L, reps = 6, weightKg = 65f)
            ))
        )
        assertEquals(65f, useCase("d1_e1"))
    }

    @Test
    fun `looks past sessions of other workouts`() = runTest {
        val workoutB = Session(2L, "workout_b", 2000L, 3000L)
        val workoutA = Session(1L, "workout_a", 0L, 1000L)
        coEvery { repo.getCompletedSince(0) } returns listOf(workoutB, workoutA)
        coEvery { repo.getEntriesForSession(2L) } returns listOf(
            ExerciseEntry(2L, 2L, "d2_e1", listOf(RepSetRecord(3L, 2L, 0L, reps = 8, weightKg = 100f)))
        )
        coEvery { repo.getEntriesForSession(1L) } returns listOf(
            ExerciseEntry(1L, 1L, "d1_e1", listOf(RepSetRecord(1L, 1L, 0L, reps = 6, weightKg = 60f)))
        )
        assertEquals(60f, useCase("d1_e1"))
    }

    @Test
    fun `ignores cardio sessions`() = runTest {
        val run = Session(3L, "", 0L, 1000L, type = "RUNNING")
        coEvery { repo.getCompletedSince(0) } returns listOf(run)
        assertNull(useCase("d1_e1"))
    }
}
