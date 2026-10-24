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
        coEvery { repo.getLastCompleted() } returns null
        assertNull(useCase("d1_e1"))
    }

    @Test
    fun `returns max weight from last session for the exercise`() = runTest {
        val session = Session(1L, "workout_a", 0L, 1000L)
        coEvery { repo.getLastCompleted() } returns session
        coEvery { repo.getEntriesForSession(1L) } returns listOf(
            ExerciseEntry(1L, 1L, "d1_e1", listOf(
                RepSetRecord(1L, 1L, 0L, reps = 6, weightKg = 60f),
                RepSetRecord(2L, 1L, 0L, reps = 6, weightKg = 65f)
            ))
        )
        assertEquals(65f, useCase("d1_e1"))
    }

    @Test
    fun `returns null when exercise not in last session`() = runTest {
        val session = Session(1L, "workout_a", 0L, 1000L)
        coEvery { repo.getLastCompleted() } returns session
        coEvery { repo.getEntriesForSession(1L) } returns listOf(
            ExerciseEntry(1L, 1L, "d1_e2", listOf(
                RepSetRecord(1L, 1L, 0L, reps = 6, weightKg = 60f)
            ))
        )
        assertNull(useCase("d1_e1"))
    }
}
