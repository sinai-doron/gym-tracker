package com.doron.gymtracker.ui.screens.home

import app.cash.turbine.test
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getNextWorkout: GetNextWorkoutUseCase
    private lateinit var startSession: StartSessionUseCase
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNextWorkout = mockk()
        startSession = mockk()
        workoutRepo = mockk()
        coEvery { getNextWorkout() } returns TrainingPlanData.workoutB
        coEvery { workoutRepo.getAll() } returns TrainingPlanData.all
        viewModel = HomeViewModel(getNextWorkout, startSession, workoutRepo)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState initially suggests next workout`() = runTest {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals("workout_b", state.suggestedWorkout?.id)
        }
    }

    @Test
    fun `selectWorkout overrides selection`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectWorkout(TrainingPlanData.workoutA)
        assertEquals("workout_a", viewModel.uiState.value.selectedWorkout?.id)
    }

    @Test
    fun `selectedWorkout defaults to suggestedWorkout`() = runTest {
        viewModel.uiState.test {
            testDispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(state.suggestedWorkout?.id, state.selectedWorkout?.id)
        }
    }
}
