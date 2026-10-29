package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.usecase.CloseStaleSessionsUseCase.Companion.MAX_SESSION_MS
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CloseStaleSessionsUseCaseTest {

    private lateinit var repo: SessionRepository
    private lateinit var useCase: CloseStaleSessionsUseCase
    private val now = 10 * MAX_SESSION_MS

    @Before
    fun setup() {
        repo = mockk(relaxed = true)
        useCase = CloseStaleSessionsUseCase(repo)
    }

    @Test
    fun `only looks at sessions started more than 24 hours ago`() = runTest {
        coEvery { repo.getUnfinishedStartedBefore(any()) } returns emptyList()
        useCase(now)
        coVerify { repo.getUnfinishedStartedBefore(now - MAX_SESSION_MS) }
    }

    @Test
    fun `stale session with sets is finished at its last set`() = runTest {
        val session = Session(1L, "workout_a", startedAt = 1_000L, finishedAt = null)
        coEvery { repo.getUnfinishedStartedBefore(any()) } returns listOf(session)
        coEvery { repo.getLastActivityAt(1L) } returns 5_000L
        useCase(now)
        coVerify { repo.closeSession(1L, 5_000L) }
        coVerify(exactly = 0) { repo.deleteSession(any()) }
    }

    @Test
    fun `stale session with nothing recorded is deleted`() = runTest {
        val session = Session(2L, "workout_c", startedAt = 1_000L, finishedAt = null)
        coEvery { repo.getUnfinishedStartedBefore(any()) } returns listOf(session)
        coEvery { repo.getLastActivityAt(2L) } returns null
        useCase(now)
        coVerify { repo.deleteSession(2L) }
        coVerify(exactly = 0) { repo.closeSession(any(), any()) }
    }

    @Test
    fun `finish time is capped at 24 hours after start`() = runTest {
        val session = Session(3L, "workout_b", startedAt = 1_000L, finishedAt = null)
        coEvery { repo.getUnfinishedStartedBefore(any()) } returns listOf(session)
        coEvery { repo.getLastActivityAt(3L) } returns 1_000L + 2 * MAX_SESSION_MS
        useCase(now)
        coVerify { repo.closeSession(3L, 1_000L + MAX_SESSION_MS) }
    }
}
