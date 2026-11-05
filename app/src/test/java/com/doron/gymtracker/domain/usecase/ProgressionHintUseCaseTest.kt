package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProgressionHintUseCaseTest {

    private lateinit var repo: SessionRepository
    private lateinit var useCase: ProgressionHintUseCase

    /** 3 sets of 6–10, on a barbell. */
    private val squat = RepExerciseDefinition(
        "d1_e1", "Squat", targetSets = 3, restDurationSecs = 120, targetReps = 10, targetRepsMin = 6, weightStepKg = 2.5
    )

    @Before
    fun setup() {
        repo = mockk()
        useCase = ProgressionHintUseCase(repo, mockk { coEvery { getActiveProgram() } returns null })
    }

    private fun repSet(id: Long, reps: Int, weight: Float) = RepSetRecord(id, id, 0L, reps, weight)

    /** A finished session that had the exercise, with its sets as (reps, weight). */
    private fun history(id: Long, vararg sets: Pair<Int, Float>) =
        ExerciseSessionSets(id, id * 1000, sets.mapIndexed { i, (reps, weight) -> repSet(id * 10 + i, reps, weight) })

    private fun given(vararg sessions: ExerciseSessionSets) {
        coEvery { repo.getExerciseHistory("d1_e1") } returns sessions.toList()
    }

    @Test
    fun `no history, no hint`() = runTest {
        given()
        assertEquals(ProgressionHint.OnTrack, useCase(squat))
    }

    @Test
    fun `every set at the top of the range adds the equipment step`() = runTest {
        given(history(1, 10 to 60f, 10 to 60f, 10 to 60f))
        assertEquals(ProgressionHint.Increase(2.5f), useCase(squat))
    }

    @Test
    fun `the step follows the equipment, not the reps`() = runTest {
        given(history(1, 10 to 20f, 10 to 20f, 10 to 20f))
        assertEquals(ProgressionHint.Increase(2f), useCase(squat.copy(weightStepKg = 2.0)))
        assertEquals(ProgressionHint.Increase(5f), useCase(squat.copy(weightStepKg = 5.0)))
    }

    @Test
    fun `an exercise whose equipment isn't known steps 2_5 kg`() = runTest {
        given(history(1, 10 to 60f, 10 to 60f, 10 to 60f))
        assertEquals(ProgressionHint.Increase(2.5f), useCase(squat.copy(weightStepKg = null)))
    }

    @Test
    fun `one set short of the top is not yet`() = runTest {
        given(history(1, 10 to 60f, 10 to 60f, 8 to 60f))
        assertEquals(ProgressionHint.OnTrack, useCase(squat))
    }

    @Test
    fun `fewer sets than planned is not yet`() = runTest {
        given(history(1, 10 to 60f, 10 to 60f))
        assertEquals(ProgressionHint.OnTrack, useCase(squat))
    }

    @Test
    fun `lighter warm-up sets don't count`() = runTest {
        given(history(1, 5 to 40f, 10 to 60f, 10 to 60f, 10 to 60f))
        assertEquals(ProgressionHint.Increase(2.5f), useCase(squat))
    }

    @Test
    fun `a fixed count is its own top`() = runTest {
        given(history(1, 8 to 60f, 8 to 60f, 8 to 60f))
        assertEquals(ProgressionHint.Increase(2.5f), useCase(squat.copy(targetReps = 8, targetRepsMin = null)))
    }

    @Test
    fun `only the last session decides`() = runTest {
        // Hit the top before, then moved up and is working back to it
        given(history(1, 10 to 60f, 10 to 60f, 10 to 60f), history(2, 7 to 62.5f, 7 to 62.5f, 6 to 62.5f))
        assertEquals(ProgressionHint.OnTrack, useCase(squat))
    }

    @Test
    fun `three sessions at the same weight and reps, short of the top, is stalled`() = runTest {
        given(history(1, 8 to 60f, 7 to 60f), history(2, 8 to 60f, 8 to 60f), history(3, 8 to 60f, 6 to 60f))
        assertEquals(ProgressionHint.Stalled, useCase(squat))
    }

    @Test
    fun `improving weight across sessions is on track`() = runTest {
        given(history(1, 8 to 50f), history(2, 8 to 55f), history(3, 8 to 60f))
        assertEquals(ProgressionHint.OnTrack, useCase(squat))
    }

    @Test
    fun `a deload week's sessions say nothing about progress`() = runTest {
        // Week 5 hit the top; week 6, the deload, was lighter and shorter on purpose
        useCase = ProgressionHintUseCase(repo, ProgramFixture.repo())
        given(
            ExerciseSessionSets(5, ProgramFixture.inWeek(5), (1..3).map { repSet(5L * 10 + it, 10, 60f) }),
            ExerciseSessionSets(6, ProgramFixture.inWeek(6), listOf(repSet(61, 5, 36f), repSet(62, 5, 36f)))
        )
        assertEquals(ProgressionHint.Increase(2.5f), useCase(squat))
    }
}
