package com.doron.gymtracker.ui.screens.home

import app.cash.turbine.test
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.StepCounterManager
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.GetWorkoutPreviewUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var getNextWorkout: GetNextWorkoutUseCase
    private lateinit var startSession: StartSessionUseCase
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var sessionRepo: SessionRepository
    private lateinit var stepCounter: StepCounterManager
    private lateinit var healthConnect: HealthConnectManager
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        getNextWorkout = mockk()
        startSession = mockk()
        workoutRepo = mockk()
        coEvery { getNextWorkout() } returns TrainingPlanData.workoutB
        coEvery { workoutRepo.getAll() } returns TrainingPlanData.all
        sessionRepo = mockk(relaxed = true)
        stepCounter = mockk(relaxed = true)
        every { stepCounter.isAvailable } returns false
        every { stepCounter.todaySteps } returns MutableStateFlow(0)
        healthConnect = mockk(relaxed = true)
        every { healthConnect.isAvailable } returns false
        val getWorkoutPreview = mockk<GetWorkoutPreviewUseCase>()
        coEvery { getWorkoutPreview(any(), any()) } answers { WorkoutPreview(firstArg(), emptyList(), null, null) }
        viewModel = HomeViewModel(getNextWorkout, startSession, workoutRepo, sessionRepo, stepCounter, healthConnect,
            getWorkoutPreview, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            mockk(relaxed = true) {
                every { stepGoal } returns flowOf(10_000)
                every { runTarget } returns flowOf(com.doron.gymtracker.domain.model.RunTarget())
            },
            mockk(relaxed = true))
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
    fun `preview is built for the suggested workout`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("workout_b", viewModel.uiState.value.nextWorkoutPreview?.workout?.id)
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
