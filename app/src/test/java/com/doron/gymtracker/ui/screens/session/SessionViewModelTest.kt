package com.doron.gymtracker.ui.screens.session

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.IsDeloadWeekUseCase
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
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.RepSetRecord
import org.junit.Assert.assertEquals
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
    private lateinit var isDeloadWeek: IsDeloadWeekUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = mockk()
        workoutRepo = mockk()
        finishSession = mockk()
        resolveVariant = mockk()
        isDeloadWeek = mockk()
        every { resolveVariant(any(), any()) } answers { firstArg() }
        every { isDeloadWeek(any()) } returns false // identity — return exercise as-is
        coEvery { workoutRepo.getById("workout_a") } returns TrainingPlanData.workoutA
        coEvery { sessionRepo.getEntriesForSession(1L) } returns emptyList()
        coEvery { sessionRepo.getById(1L) } returns Session(1L, "workout_a", System.currentTimeMillis(), null)
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        coJustRun { finishSession(1L) }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeViewModel() =
        SessionViewModel(1L, "workout_a", sessionRepo, workoutRepo, finishSession, resolveVariant, isDeloadWeek)

    @Test
    fun `elapsedSecs counts from the session's start, so a resumed session keeps its time`() = runTest(testDispatcher) {
        coEvery { sessionRepo.getById(1L) } returns
            Session(1L, "workout_a", System.currentTimeMillis() - 600_000, null)
        val viewModel = makeViewModel()
        advanceTimeBy(100)
        val elapsed = viewModel.uiState.value.elapsedSecs
        assertTrue("Expected about 600s, got $elapsed", elapsed in 599..605)
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
    fun `resumed session restores the sets already logged`() = runTest(testDispatcher) {
        val set = RepSetRecord(7L, 3L, 0L, reps = 10, weightKg = 40f)
        coEvery { sessionRepo.getEntriesForSession(1L) } returns listOf(ExerciseEntry(3L, 1L, "d1_e1", listOf(set)))
        val viewModel = makeViewModel()
        advanceTimeBy(100)
        assertEquals(listOf(set), viewModel.uiState.value.loggedSets["d1_e1"])
        viewModel.finishSession()
        advanceTimeBy(100)
    }

    @Test
    fun `finish session without logging any exercise succeeds`() = runTest(testDispatcher) {
        val viewModel = makeViewModel()
        viewModel.finishSession()
        advanceTimeBy(100)
        assertTrue(viewModel.uiState.value.isFinished)
    }
}
