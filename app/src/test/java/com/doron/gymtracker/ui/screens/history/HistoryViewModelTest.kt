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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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

    @Test
    fun `lastSession is empty when no sessions exist`() = runTest(testDispatcher) {
        coEvery { sessionRepo.getLastCompleted() } returns null
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(vm.lastSession.value.workoutLabel)
        assertTrue(vm.lastSession.value.summaries.isEmpty())
    }

    @Test
    fun `trends is empty when no sessions exist`() = runTest(testDispatcher) {
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        coEvery { sessionRepo.getLastCompleted() } returns null
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.trends.value.dataPoints.isEmpty())
    }

    @Test
    fun `selectExerciseForTrend updates selectedExercise`() = runTest(testDispatcher) {
        coEvery { sessionRepo.observeAll() } returns flowOf(emptyList())
        coEvery { sessionRepo.getLastCompleted() } returns null
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        testDispatcher.scheduler.advanceUntilIdle()
        vm.selectExerciseForTrend("Squat")
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.trends.value.selectedExercise == "Squat")
    }

    @Test
    fun `trends dataPoints ordered ascending by date when sessions exist`() = runTest(testDispatcher) {
        val session1 = Session(1L, "workout_a", startedAt = 1_000_000L, finishedAt = 1_001_000L)
        val session2 = Session(2L, "workout_a", startedAt = 2_000_000L, finishedAt = 2_001_000L)
        val entry1 = ExerciseEntry(1L, 1L, "d1_e1", listOf(
            RepSetRecord(1L, 1L, 1_000_500L, reps = 8, weightKg = 60f)
        ))
        val entry2 = ExerciseEntry(2L, 2L, "d1_e1", listOf(
            RepSetRecord(2L, 2L, 2_000_500L, reps = 8, weightKg = 65f)
        ))
        coEvery { sessionRepo.observeAll() } returns flowOf(listOf(session2, session1))
        coEvery { sessionRepo.getLastCompleted() } returns session2
        coEvery { sessionRepo.getEntriesForSession(1L) } returns listOf(entry1)
        coEvery { sessionRepo.getEntriesForSession(2L) } returns listOf(entry2)
        coEvery { workoutRepo.getById("workout_a") } returns TrainingPlanData.workoutA
        val vm = HistoryViewModel(sessionRepo, workoutRepo)
        vm.selectExerciseForTrend("לחיצת חזה עם מוט (שטוח)")
        testDispatcher.scheduler.advanceUntilIdle()
        val points = vm.trends.value.dataPoints
        assertTrue(points.size == 2)
        assertTrue(points[0].dateLabel <= points[1].dateLabel)
    }
}
