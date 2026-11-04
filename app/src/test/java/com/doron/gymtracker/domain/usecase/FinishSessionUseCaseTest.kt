package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.data.seed.TrainingPlanData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FinishSessionUseCaseTest {

    private val sessions = mockk<SessionRepository>(relaxed = true)
    private val workouts = mockk<WorkoutRepository>()
    private val healthConnect = mockk<HealthConnectManager>(relaxed = true)
    private val finish = FinishSessionUseCase(sessions, workouts, healthConnect)

    @Test
    fun `a finished workout goes to Health Connect, read by its own id`() = runTest {
        coEvery { sessions.getById(5) } returns Session(5, "workout_a", 1_000, 9_000)
        coEvery { workouts.getById("workout_a") } returns TrainingPlanData.workoutA
        finish(5, "A")
        coVerify { healthConnect.writeWorkoutSession(1_000, 9_000, "A") }
        coVerify(exactly = 0) { sessions.getLastCompleted() }
    }

    @Test
    fun `one logged by hand was written when it was logged`() = runTest {
        coEvery { sessions.getById(6) } returns Session(6, "workout_a", 1_000, 9_000, loggedManually = true)
        finish(6, "A")
        coVerify(exactly = 0) { healthConnect.writeWorkoutSession(any(), any(), any(), any()) }
    }
}
