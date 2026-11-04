package com.doron.gymtracker.ui.screens.history

import io.mockk.every
import com.doron.gymtracker.data.templates.TemplateNames
import com.doron.gymtracker.R
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.TrainingTypes
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.TrainingType
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
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    // Names come back as saved: these tests aren't about translating template names
    private val sameNames = mockk<TemplateNames> {
        every { workout(any(), any()) } answers { firstArg() }
        every { program(any(), any()) } answers { firstArg() }
    }


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
        return HistoryViewModel(sessionRepo, workoutRepo, mockk(relaxed = true), sameNames)
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
        val vm = HistoryViewModel(sessionRepo, workoutRepo, mockk(relaxed = true), sameNames)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.sessionDates.value.isNotEmpty())
    }

    @Test
    fun `each day lists the training types done on it`() = runTest(testDispatcher) {
        val gym = Session(1L, "workout_a", startedAt = 0L, finishedAt = 1000L)
        val run = Session(2L, "RUNNING", startedAt = 2000L, finishedAt = 3000L, type = "RUNNING", activityTypeId = "run")
        val pilates = Session(3L, "OTHER", startedAt = 2 * 86_400_000L, finishedAt = 2 * 86_400_000L + 1000, type = "OTHER", activityTypeId = "pilates")
        coEvery { sessionRepo.observeAll() } returns flowOf(listOf(gym, run, pilates))
        coEvery { sessionRepo.getLastCompleted() } returns pilates
        val types = mockk<TrainingTypes>()
        coEvery { types.all() } returns listOf(type("strength", 1), type("run", 2), type("pilates", 10))
        val vm = HistoryViewModel(sessionRepo, workoutRepo, types, sameNames)
        testDispatcher.scheduler.advanceUntilIdle()
        val days = vm.sessionDates.value.values.map { day -> day.map { it.id }.toSet() }
        assertTrue(setOf("strength", "run") in days)
        assertTrue(setOf("pilates") in days)
    }

    private fun type(id: String, order: Int) =
        TrainingType(id, null, id, "#123456", Recorder.DURATION, null, null, true, order, false)

    @Test
    fun `a run is named by a string resource`() = runTest(testDispatcher) {
        val run = Session(2L, "RUNNING", startedAt = 2000L, finishedAt = 3000L, type = "RUNNING")
        coEvery { sessionRepo.observeAll() } returns flowOf(listOf(run))
        coEvery { sessionRepo.getLastCompleted() } returns run
        val vm = HistoryViewModel(sessionRepo, workoutRepo, mockk(relaxed = true), sameNames)
        vm.loadDaySessions(Instant.ofEpochMilli(2000L).atZone(ZoneId.systemDefault()).toLocalDate())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(R.string.history_run, vm.selectedDaySessions.value?.sessions?.single()?.workoutNameRes)
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
