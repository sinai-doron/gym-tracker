package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RunBadge
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GetRunBadgesUseCaseTest {

    private fun run(id: Long, startedAt: Long, meters: Double, type: String = "RUNNING") =
        Session(id, type, startedAt, startedAt + 1_000, type = type, distanceMeters = meters)

    private fun useCase(vararg sessions: Session): GetRunBadgesUseCase {
        val repo = mockk<SessionRepository>()
        coEvery { repo.getCompletedSince(0) } returns sessions.toList().sortedByDescending { it.startedAt }
        return GetRunBadgesUseCase(repo)
    }

    @Test
    fun `the first run to reach a distance earns its badge`() = runTest {
        val badges = useCase(run(1, 100, 1_200.0), run(2, 200, 5_300.0), run(3, 300, 6_000.0))()
        assertEquals(1L, badges.first { it.badge == RunBadge.ONE_K }.sessionId)
        assertEquals(2L, badges.first { it.badge == RunBadge.FIVE_K }.sessionId)
        assertNull(badges.first { it.badge == RunBadge.TEN_K }.sessionId)
    }

    @Test
    fun `one long run can earn several badges at once`() = runTest {
        assertEquals(
            listOf(RunBadge.ONE_K, RunBadge.FIVE_K, RunBadge.TEN_K),
            useCase(run(7, 100, 10_050.0)).earnedBy(7)
        )
    }

    @Test
    fun `walks and runs after the first don't earn badges`() = runTest {
        val uc = useCase(run(1, 100, 6_000.0, type = "WALKING"), run(2, 200, 1_100.0), run(3, 300, 1_500.0))
        assertEquals(listOf(RunBadge.ONE_K), uc.earnedBy(2))
        assertEquals(emptyList<RunBadge>(), uc.earnedBy(3))
    }
}
