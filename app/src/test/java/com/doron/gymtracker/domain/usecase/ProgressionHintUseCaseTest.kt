package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressionHintUseCaseTest {

    private lateinit var repo: SessionRepository
    private lateinit var useCase: ProgressionHintUseCase

    @Before
    fun setup() {
        repo = mockk()
        useCase = ProgressionHintUseCase(repo, mockk { coEvery { getActiveProgram() } returns null })
    }

    private fun repSet(id: Long, reps: Int, weight: Float) = RepSetRecord(id, id, 0L, reps, weight)

    /** A finished session that had the exercise, with its sets as (reps, weight). */
    private fun history(id: Long, vararg sets: Pair<Int, Float>) =
        ExerciseSessionSets(id, id * 1000, sets.mapIndexed { i, (reps, weight) -> repSet(id * 10 + i, reps, weight) })

    @Test
    fun `OnTrack when fewer than 3 sessions`() = runTest {
        coEvery { repo.getExerciseHistory("d1_e1") } returns listOf(history(1, 6 to 60f, 6 to 60f))
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue(hint is ProgressionHint.OnTrack)
    }

    @Test
    fun `Increase when last 3 sessions all hit top of rep range`() = runTest {
        coEvery { repo.getExerciseHistory("d1_e1") } returns (1L..3L).map { history(it, 8 to 60f, 8 to 60f) }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Increase but got $hint", hint is ProgressionHint.Increase)
    }

    @Test
    fun `Stalled when last 3 sessions have same weight and same reps below target`() = runTest {
        coEvery { repo.getExerciseHistory("d1_e1") } returns (1L..3L).map { history(it, 6 to 60f, 6 to 60f) }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Stalled but got $hint", hint is ProgressionHint.Stalled)
    }

    @Test
    fun `OnTrack when improving weight across sessions`() = runTest {
        // Different weights each session — not stalled
        coEvery { repo.getExerciseHistory("d1_e1") } returns listOf(
            history(1, 6 to 50f), history(2, 6 to 55f), history(3, 6 to 60f)
        )
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue(hint is ProgressionHint.OnTrack)
    }

    @Test
    fun `Only the last 3 sessions that had the exercise count`() = runTest {
        // An early session missed the target; the last three hit it
        coEvery { repo.getExerciseHistory("d1_e1") } returns listOf(
            history(1, 5 to 60f), history(4, 8 to 60f), history(7, 8 to 60f), history(10, 8 to 60f)
        )
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Increase but got $hint", hint is ProgressionHint.Increase)
    }

    /**
     * The bug: the hint took the last 3 sessions of any kind. With workouts B and C and a run after
     * the last A, none of them had the bench press, so the hint was stuck on "on track".
     */
    @Test
    fun `Other workouts and runs in between don't hide the hint`() = runTest {
        coEvery { repo.observeAll() } returns flowOf(
            listOf(
                Session(1, "workout_a", 1000, 2000), Session(2, "workout_b", 3000, 4000),
                Session(3, "workout_a", 5000, 6000), Session(4, "RUNNING", 7000, 8000, "RUNNING"),
                Session(5, "workout_a", 9000, 10000), Session(6, "workout_c", 11000, 12000),
                Session(7, "RUNNING", 13000, 14000, "RUNNING"),
            )
        )
        coEvery { repo.getExerciseHistory("d1_e1") } returns listOf(
            history(1, 8 to 60f), history(3, 8 to 60f), history(5, 8 to 60f)
        )
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Increase but got $hint", hint is ProgressionHint.Increase)
    }

    @Test
    fun `a deload week's sessions say nothing about progress`() = runTest {
        // Weeks 4, 5 and 7 hit the target; week 6, the deload, was lighter and shorter on purpose
        useCase = ProgressionHintUseCase(repo, ProgramFixture.repo())
        coEvery { repo.getExerciseHistory("d1_e1") } returns listOf(4, 5, 6, 7).map { week ->
            val (reps, weight) = if (week == 6) 5 to 36f else 8 to 60f
            ExerciseSessionSets(week.toLong(), ProgramFixture.inWeek(week), listOf(repSet(week.toLong(), reps, weight)))
        }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Increase but got $hint", hint is ProgressionHint.Increase)
    }
}
