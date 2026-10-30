package com.doron.gymtracker.ui.screens.history

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionRepo: SessionRepository
    private lateinit var workoutRepo: WorkoutRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepo = mockk()
        workoutRepo = mockk()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun makeVm(): HistoryViewModel {
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        coEvery { sessionRepo.getLastCompleted() } returns null
        return HistoryViewModel(sessionRepo, workoutRepo)
    }

    @Test
    fun `currentMonth defaults to this month`() = runTest(testDispatcher) {
        val vm = makeVm()
        assertEquals(YearMonth.now(), vm.currentMonth.value)
    }

    @Test
    fun `prevMonth decrements month`() = runTest(testDispatcher) {
        val vm = makeVm()
        val before = vm.currentMonth.value
        vm.prevMonth()
        assertEquals(before.minusMonths(1), vm.currentMonth.value)
    }

    @Test
    fun `nextMonth increments month`() = runTest(testDispatcher) {
        val vm = makeVm()
        val before = vm.currentMonth.value
        vm.nextMonth()
        assertEquals(before.plusMonths(1), vm.currentMonth.value)
    }

    @Test
    fun `sessionDates is empty when no completed sessions`() = runTest(testDispatcher) {
        val vm = makeVm()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.sessionDates.value.isEmpty())
    }

    @Test
    fun `sessionDates contains date for each completed session`() = runTest(testDispatcher) {
        // epoch 0 = 1970-01-01
        val session = Session(1L, "workout_a", startedAt = 0L, finishedAt = 1000L)
        coEvery { sessionRepo.observeAll() } returns flowOf(listOf(session))
        coEvery { sessionRepo.getLastCompleted() } returns session
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.sessionDates.value.isNotEmpty())
    }

    @Test
    fun `a day with a gym session and a run is marked as both`() = runTest(testDispatcher) {
        val gym = Session(1L, "workout_a", startedAt = 0L, finishedAt = 1000L)
        val run = Session(2L, "RUNNING", startedAt = 2000L, finishedAt = 3000L, type = "RUNNING")
        val walk = Session(3L, "WALKING", startedAt = 2 * 86_400_000L, finishedAt = 2 * 86_400_000L + 1000, type = "WALKING")
        coEvery { sessionRepo.observeAll() } returns flowOf(listOf(gym, run, walk))
        coEvery { sessionRepo.getLastCompleted() } returns walk
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        val days = vm.sessionDates.value.values.toList()
        assertTrue(setOf(DayKind.GYM, DayKind.CARDIO) in days)
        assertTrue(setOf(DayKind.CARDIO) in days)
    }

    @Test
    fun `selectedDaySessions is null initially`() = runTest(testDispatcher) {
        val vm = makeVm()
        assertNull(vm.selectedDaySessions.value)
    }

    @Test
    fun `clearSelectedDay sets selectedDaySessions to null`() = runTest(testDispatcher) {
        val vm = makeVm()
        vm.clearSelectedDay()
        assertNull(vm.selectedDaySessions.value)
    }
}
