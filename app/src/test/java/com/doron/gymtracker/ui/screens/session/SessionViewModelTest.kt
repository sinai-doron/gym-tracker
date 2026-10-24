package com.doron.gymtracker.ui.screens.session

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.ResolveExerciseVariantUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepo: SessionRepository
    private lateinit var workoutRepo: WorkoutRepository
    private lateinit var finishSession: FinishSessionUseCase
    private lateinit var resolveVariant: ResolveExerciseVariantUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = mockk()
        workoutRepo = mockk()
        finishSession = mockk()
        resolveVariant = mockk()
        every { resolveVariant(any(), any()) } answers { firstArg() } // identity — return exercise as-is
        coEvery { workoutRepo.getById("workout_a") } returns TrainingPlanData.workoutA
        coEvery { sessionRepo.getEntriesForSession(1L) } returns emptyList()
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        coJustRun { finishSession(1L) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel() =
        SessionViewModel(1L, "workout_a", sessionRepo, workoutRepo, finishSession, resolveVariant)

    @Test
    fun `elapsedSecs increments each second`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        val t0 = viewModel.uiState.value.elapsedSecs
        advanceTimeBy(2_100)
        val t1 = viewModel.uiState.value.elapsedSecs
        assertTrue("Expected t1 ($t1) >= t0 ($t0) + 2", t1 >= t0 + 2)
        // finish so the ticker loop exits and runTest can complete
        viewModel.finishSession()
        advanceTimeBy(100)
    }

    @Test
    fun `finishSession sets isFinished true`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        advanceTimeBy(100)
        viewModel.finishSession()
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isFinished)
    }

    @Test
    fun `finish session without logging any exercise succeeds`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        viewModel.finishSession()
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isFinished)
    }
}
