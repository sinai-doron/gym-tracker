package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class GetNextWorkoutUseCaseTest {

    private val sessions = mockk<SessionRepository>()
    private val repo = mockk<WorkoutRepository>().also { coEvery { it.getAll() } returns TrainingPlanData.all }
    private val useCase = GetNextWorkoutUseCase(sessions, repo)

    /** Finished sessions, newest first, as getCompletedSince returns them. */
    private fun history(vararg workoutIds: String) {
        coEvery { sessions.getCompletedSince(0) } returns workoutIds.mapIndexed { i, id ->
            val type = if (id == "RUNNING") "RUNNING" else "GYM"
            Session(i + 1L, id, startedAt = 1_000L - i, finishedAt = 2_000L - i, type = type)
        }
    }

    @Test
    fun `returns A when no prior session`() = runTest {
        history()
        assertEquals("A", useCase()?.code)
    }

    @Test
    fun `returns B after A`() = runTest {
        history("workout_a")
        assertEquals("B", useCase()?.code)
    }

    @Test
    fun `returns C after B`() = runTest {
        history("workout_b", "workout_a")
        assertEquals("C", useCase()?.code)
    }

    @Test
    fun `returns A after C — wraps around`() = runTest {
        history("workout_c")
        assertEquals("A", useCase()?.code)
    }

    @Test
    fun `a run since the last workout doesn't move the rotation`() = runTest {
        history("RUNNING", "workout_a")
        assertEquals("B", useCase()?.code)
    }

    @Test
    fun `a workout taken out of the rotation is passed over`() = runTest {
        // workout_x was hidden after it was done: the one before it decides
        history("workout_x", "workout_b")
        assertEquals("C", useCase()?.code)
    }

    // ---- By weekday ----

    /** Thursday 5 November 2026. */
    private val thursday = LocalDate.of(2026, 11, 5)

    /** A on Monday, B on Thursday, C with no day. */
    private fun weekly() {
        val (a, b, c) = TrainingPlanData.all
        coEvery { repo.getAll() } returns listOf(a.copy(weekday = DayOfWeek.MONDAY), b.copy(weekday = DayOfWeek.THURSDAY), c)
    }

    /** A finished session of [workoutId] that started at noon UTC on [day]. */
    private fun doneOn(vararg done: Pair<String, LocalDate>) {
        coEvery { sessions.getCompletedSince(0) } returns done.mapIndexed { i, (id, day) ->
            val at = day.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
            Session(i + 1L, id, startedAt = at, finishedAt = at + 3_600_000)
        }
    }

    private suspend fun next() = useCase.next(thursday, ZoneOffset.UTC)

    @Test
    fun `on a workout's day, that workout is next, today`() = runTest {
        weekly()
        doneOn("workout_a" to thursday.minusDays(3))
        val next = next()
        assertEquals("B", next?.workout?.code)
        assertEquals(thursday, next?.day)
    }

    @Test
    fun `once today's workout is done, the next day with one`() = runTest {
        weekly()
        doneOn("workout_b" to thursday)
        val next = next()
        assertEquals("A", next?.workout?.code)
        assertEquals(LocalDate.of(2026, 11, 9), next?.day)
    }

    @Test
    fun `a missed day isn't made up`() = runTest {
        // Monday's A was skipped; Thursday is still B's day
        weekly()
        doneOn("workout_b" to thursday.minusDays(7))
        assertEquals("B", next()?.workout?.code)
    }

    @Test
    fun `a workout without a day is never suggested`() = runTest {
        weekly()
        doneOn("workout_b" to thursday)
        assertEquals("A", next()?.workout?.code)
        coEvery { repo.getAll() } returns TrainingPlanData.all.map { if (it.code == "C") it.copy(weekday = DayOfWeek.SATURDAY) else it }
        assertEquals("C", next()?.workout?.code)
    }

    @Test
    fun `a programme with one day comes round again next week`() = runTest {
        val (a, b, c) = TrainingPlanData.all
        coEvery { repo.getAll() } returns listOf(a, b.copy(weekday = DayOfWeek.THURSDAY), c)
        doneOn("workout_b" to thursday)
        assertEquals(thursday.plusDays(7), next()?.day)
    }

    @Test
    fun `two workouts on one day come in their order`() = runTest {
        val (a, b, c) = TrainingPlanData.all
        coEvery { repo.getAll() } returns listOf(a.copy(weekday = DayOfWeek.THURSDAY), b.copy(weekday = DayOfWeek.THURSDAY), c)
        doneOn()
        assertEquals("A", next()?.workout?.code)
        doneOn("workout_a" to thursday)
        assertEquals("B", next()?.workout?.code)
    }

    @Test
    fun `in order, no day is given`() = runTest {
        history("workout_a")
        assertNull(next()?.day)
    }

    @Test
    fun `a programme with no workouts yet has no next workout`() = runTest {
        coEvery { repo.getAll() } returns emptyList()
        history("workout_a")
        assertNull(useCase())
    }
}
