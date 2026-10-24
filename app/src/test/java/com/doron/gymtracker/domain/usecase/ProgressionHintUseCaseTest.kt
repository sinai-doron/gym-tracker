package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProgressionHintUseCaseTest {

    private lateinit var repo: SessionRepository
    private lateinit var useCase: ProgressionHintUseCase

    @Before
    fun setup() {
        repo = mockk()
        useCase = ProgressionHintUseCase(repo)
    }

    private fun session(id: Long, startedAt: Long) = Session(id, "workout_a", startedAt, startedAt + 3600_000)
    private fun repSet(id: Long, entryId: Long, reps: Int, weight: Float) =
        RepSetRecord(id, entryId, 0L, reps, weight)
    private fun entry(id: Long, sessionId: Long, exerciseId: String, vararg sets: RepSetRecord) =
        ExerciseEntry(id, sessionId, exerciseId, sets.toList())

    @Test
    fun `OnTrack when fewer than 3 sessions`() = runTest {
        coEvery { repo.observeAll() } returns flowOf(listOf(session(1, 0)))
        coEvery { repo.getEntriesForSession(1) } returns listOf(
            entry(1, 1, "d1_e1", repSet(1, 1, 6, 60f), repSet(2, 1, 6, 60f))
        )
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue(hint is ProgressionHint.OnTrack)
    }

    @Test
    fun `Increase when last 3 sessions all hit top of rep range`() = runTest {
        val sessions = listOf(session(1, 0), session(2, 1000), session(3, 2000))
        coEvery { repo.observeAll() } returns flowOf(sessions)
        sessions.forEach { s ->
            coEvery { repo.getEntriesForSession(s.id) } returns listOf(
                entry(s.id, s.id, "d1_e1",
                    repSet(s.id * 10, s.id, 8, 60f),
                    repSet(s.id * 10 + 1, s.id, 8, 60f)
                )
            )
        }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Increase but got $hint", hint is ProgressionHint.Increase)
    }

    @Test
    fun `Stalled when last 3 sessions have same weight and same reps below target`() = runTest {
        val sessions = listOf(session(1, 0), session(2, 1000), session(3, 2000))
        coEvery { repo.observeAll() } returns flowOf(sessions)
        sessions.forEach { s ->
            coEvery { repo.getEntriesForSession(s.id) } returns listOf(
                entry(s.id, s.id, "d1_e1",
                    repSet(s.id * 10, s.id, 6, 60f),
                    repSet(s.id * 10 + 1, s.id, 6, 60f)
                )
            )
        }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue("Expected Stalled but got $hint", hint is ProgressionHint.Stalled)
    }

    @Test
    fun `OnTrack when improving weight across sessions`() = runTest {
        val sessions = listOf(session(1, 0), session(2, 1000), session(3, 2000))
        coEvery { repo.observeAll() } returns flowOf(sessions)
        // Different weights each session — not stalled
        listOf(50f, 55f, 60f).forEachIndexed { i, weight ->
            val s = sessions[i]
            coEvery { repo.getEntriesForSession(s.id) } returns listOf(
                entry(s.id, s.id, "d1_e1", repSet(s.id * 10, s.id, 6, weight))
            )
        }
        val hint = useCase("d1_e1", targetReps = 8)
        assertTrue(hint is ProgressionHint.OnTrack)
    }
}
