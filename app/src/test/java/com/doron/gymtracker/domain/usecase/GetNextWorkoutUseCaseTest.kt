package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.RotationPrefs
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.WorkoutLabel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetNextWorkoutUseCaseTest {

    private lateinit var prefs: RotationPrefs
    private lateinit var repo: WorkoutRepository
    private lateinit var useCase: GetNextWorkoutUseCase

    @Before
    fun setup() {
        prefs = mockk()
        repo = mockk()
        coEvery { repo.getAll() } returns TrainingPlanData.all
        useCase = GetNextWorkoutUseCase(prefs, repo)
    }

    @Test
    fun `returns A when no prior session`() = runTest {
        coEvery { prefs.getLastLabel() } returns null
        assertEquals(WorkoutLabel.A, useCase().label)
    }

    @Test
    fun `returns B after A`() = runTest {
        coEvery { prefs.getLastLabel() } returns WorkoutLabel.A
        assertEquals(WorkoutLabel.B, useCase().label)
    }

    @Test
    fun `returns C after B`() = runTest {
        coEvery { prefs.getLastLabel() } returns WorkoutLabel.B
        assertEquals(WorkoutLabel.C, useCase().label)
    }

    @Test
    fun `returns A after C — wraps around`() = runTest {
        coEvery { prefs.getLastLabel() } returns WorkoutLabel.C
        assertEquals(WorkoutLabel.A, useCase().label)
    }
}
