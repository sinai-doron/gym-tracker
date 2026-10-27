package com.doron.gymtracker.ui.screens.session

import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var logSet: LogSetUseCase

    // Rep exercise from TrainingPlanData (restDurationSecs = 90)
    private val repExercise = TrainingPlanData.workoutA.exercises.first() // ex_a1, rep-based, rest=90s
    // Timed exercise from TrainingPlanData
    private val timedExercise = TrainingPlanData.workoutA.exercises.last() // Plank, rest=60s

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        logSet = mockk()
        coEvery { logSet(any(), any(), any()) } returns 1L
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `logRepSet saves set and starts rest countdown`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, repExercise, logSet)
        vm.logRepSet(8, 80f)
        advanceTimeBy(50)
        val state = vm.uiState.value
        assertEquals(1, state.completedSets.size)
        assertNotNull(state.restCountdownSecs)
        assertEquals(repExercise.restDurationSecs, state.restCountdownSecs)
        vm.dismissRestTimer()
    }

    @Test
    fun `rest countdown decrements and clears after duration`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, repExercise, logSet)
        vm.logRepSet(8, 80f)
        advanceTimeBy(50)
        assertNotNull(vm.uiState.value.restCountdownSecs)
        // advance past the full rest duration (90s)
        advanceTimeBy((repExercise.restDurationSecs * 1000 + 1000).toLong())
        assertNull(vm.uiState.value.restCountdownSecs)
    }

    @Test
    fun `dismissRestTimer clears countdown immediately`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, repExercise, logSet)
        vm.logRepSet(8, 80f)
        advanceTimeBy(50)
        vm.dismissRestTimer()
        advanceTimeBy(50)
        assertNull(vm.uiState.value.restCountdownSecs)
    }

    @Test
    fun `stopTimedSet records a TimedSetRecord and starts rest`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, timedExercise, logSet)
        vm.startTimedSet()
        advanceTimeBy(50) // let timer coroutine start
        vm.stopTimedSet()
        advanceTimeBy(50)
        val state = vm.uiState.value
        assertTrue(state.completedSets.isNotEmpty())
        val set = state.completedSets.first()
        assertTrue(set is TimedSetRecord)
        assertTrue((set as TimedSetRecord).durationSecs >= 1) // coerceAtLeast(1) guaranteed
        advanceTimeBy(100)
        vm.dismissRestTimer()
    }

    @Test
    fun `stopTimedSet is no-op if timer not running`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, timedExercise, logSet)
        vm.stopTimedSet()
        advanceTimeBy(50)
        assertTrue(vm.uiState.value.completedSets.isEmpty())
    }

    @Test
    fun `logging a set on a second exercise replaces the running rest timer`() = runTest(testDispatcher) {
        val restTimer = RestTimer(backgroundScope)
        val vmA = ExerciseDetailViewModel(1L, repExercise, logSet, restTimer = restTimer)
        val vmB = ExerciseDetailViewModel(1L, timedExercise, logSet, restTimer = restTimer)
        vmA.logRepSet(8, 80f)
        advanceTimeBy(10_050)
        vmB.startTimedSet()
        advanceTimeBy(50)
        vmB.stopTimedSet()
        advanceTimeBy(50)
        assertEquals(timedExercise.restDurationSecs, restTimer.state.value?.remainingSecs)
        assertEquals(timedExercise.restDurationSecs, vmA.uiState.value.restCountdownSecs)
        assertEquals(timedExercise.restDurationSecs, vmB.uiState.value.restCountdownSecs)
        restTimer.dismiss()
    }

    @Test
    fun `changeTargetSets updates the exercise and reports the new count`() = runTest(testDispatcher) {
        var saved: Pair<String, Int>? = null
        val vm = ExerciseDetailViewModel(1L, repExercise, logSet,
            onTargetSetsChanged = { id, sets -> saved = id to sets })
        vm.changeTargetSets(1)
        assertEquals(repExercise.targetSets + 1, vm.uiState.value.exercise.targetSets)
        assertEquals(repExercise.id to repExercise.targetSets + 1, saved)
    }

    @Test
    fun `changeTargetSets never goes below one set`() = runTest(testDispatcher) {
        val vm = ExerciseDetailViewModel(1L, repExercise, logSet)
        repeat(20) { vm.changeTargetSets(-1) }
        assertEquals(1, vm.uiState.value.exercise.targetSets)
    }
}
