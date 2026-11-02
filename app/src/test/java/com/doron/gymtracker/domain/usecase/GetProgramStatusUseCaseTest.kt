package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.domain.usecase.ProgramFixture.inWeek
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProgramStatusUseCaseTest {

    private val repo = ProgramFixture.repo()
    private val useCase = GetProgramStatusUseCase(repo)

    @Test
    fun `first week is week A, five weeks before deload`() = runTest {
        val status = useCase(inWeek(1))
        assertEquals(1, status.weekNumber)
        assertFalse(status.isWeekB)
        assertFalse(status.isDeloadWeek)
        assertEquals(5, status.weeksUntilDeload)
    }

    @Test
    fun `second week is week B`() = runTest { assertTrue(useCase(inWeek(2)).isWeekB) }

    @Test
    fun `sixth week is a deload week`() = runTest {
        val status = useCase(inWeek(6))
        assertTrue(status.isDeloadWeek)
        assertEquals(0, status.weeksUntilDeload)
    }

    @Test
    fun `countdown restarts after a deload`() = runTest { assertEquals(5, useCase(inWeek(7)).weeksUntilDeload) }

    @Test
    fun `a programme without deloads has no countdown`() = runTest {
        coEvery { repo.getActiveProgram() } returns ProgramFixture.program.copy(deloadEveryWeeks = null)
        val status = useCase(inWeek(6))
        assertFalse(status.isDeloadWeek)
        assertNull(status.weeksUntilDeload)
    }
}
