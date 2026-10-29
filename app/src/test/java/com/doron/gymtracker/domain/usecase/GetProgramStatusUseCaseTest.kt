package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetProgramStatusUseCaseTest {

    private val prefs = ProgramPrefs()
    private val useCase = GetProgramStatusUseCase(prefs)
    private val weekMs = 7L * 24 * 60 * 60 * 1000
    private fun inWeek(n: Int) = prefs.programStartEpochMs + (n - 1) * weekMs + 1_000

    @Test
    fun `first week is week A, five weeks before deload`() {
        val status = useCase(inWeek(1))
        assertEquals(1, status.weekNumber)
        assertFalse(status.isWeekB)
        assertFalse(status.isDeloadWeek)
        assertEquals(5, status.weeksUntilDeload)
    }

    @Test
    fun `second week is week B`() = assertTrue(useCase(inWeek(2)).isWeekB)

    @Test
    fun `sixth week is a deload week`() {
        val status = useCase(inWeek(6))
        assertTrue(status.isDeloadWeek)
        assertEquals(0, status.weeksUntilDeload)
    }

    @Test
    fun `countdown restarts after a deload`() = assertEquals(5, useCase(inWeek(7)).weeksUntilDeload)
}
