package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class IsDeloadWeekUseCaseTest {

    private lateinit var prefs: ProgramPrefs
    private lateinit var useCase: IsDeloadWeekUseCase

    private val startDate = LocalDate.of(2026, 10, 26)
    private val startEpoch = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    private val msPerWeek = 7L * 24 * 60 * 60 * 1000

    @Before
    fun setup() {
        prefs = mockk()
        every { prefs.programStartEpochMs } returns startEpoch
        every { prefs.weekNumber(any()) } answers { callOriginal() }
        useCase = IsDeloadWeekUseCase(prefs)
    }

    @Test
    fun `week 1 is not deload`() {
        assertFalse(useCase(startEpoch))
    }

    @Test
    fun `week 6 is deload`() {
        val week6 = startEpoch + (5 * msPerWeek)
        assertTrue(useCase(week6))
    }

    @Test
    fun `week 7 is not deload`() {
        val week7 = startEpoch + (6 * msPerWeek)
        assertFalse(useCase(week7))
    }

    @Test
    fun `week 12 is deload`() {
        val week12 = startEpoch + (11 * msPerWeek)
        assertTrue(useCase(week12))
    }

    @Test
    fun `week 5 is not deload`() {
        val week5 = startEpoch + (4 * msPerWeek)
        assertFalse(useCase(week5))
    }
}
