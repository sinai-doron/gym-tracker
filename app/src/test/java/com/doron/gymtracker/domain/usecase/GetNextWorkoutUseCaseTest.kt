package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetNextWorkoutUseCaseTest {

    private val sessions = mockk<SessionRepository>()
    private val repo = mockk<WorkoutRepository>().also { coEvery { it.getAll() } returns TrainingPlanData.all }
    private val useCase = GetNextWorkoutUseCase(sessions, repo)

    /** Finished sessions, newest first, as getCompletedSince returns them. */
    private fun history(vararg workoutIds: String) {
        coEvery { sessions.getCompletedSince(0) } returns workoutIds.mapIndexed { i, id ->
            val type = if (id == "RUNNING") "RUNNING" else "GYM"
            Session(i + 1L, id, startedAt = 1_000L - i, finishedAt = 2_000L - i, type = type)
        }
    }

    @Test
    fun `returns A when no prior session`() = runTest {
        history()
        assertEquals("A", useCase().code)
    }

    @Test
    fun `returns B after A`() = runTest {
        history("workout_a")
        assertEquals("B", useCase().code)
    }

    @Test
    fun `returns C after B`() = runTest {
        history("workout_b", "workout_a")
        assertEquals("C", useCase().code)
    }

    @Test
    fun `returns A after C — wraps around`() = runTest {
        history("workout_c")
        assertEquals("A", useCase().code)
    }

    @Test
    fun `a run since the last workout doesn't move the rotation`() = runTest {
        history("RUNNING", "workout_a")
        assertEquals("B", useCase().code)
    }
}
