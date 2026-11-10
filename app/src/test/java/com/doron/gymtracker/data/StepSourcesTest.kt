package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StepSourcesTest {

    private val sunday = LocalDate.of(2026, 11, 8)
    private val monday = sunday.plusDays(1)

    @Test
    fun `the same steps from the phone and Samsung Health count once`() {
        // As read from a Pixel 10 on 9 November: Health Connect's total said 27,910
        val days = StepSources.perDay(
            mapOf(
                "com.android.healthconnect.phone" to mapOf(monday to 15_438L),
                "com.sec.android.app.shealth" to mapOf(monday to 15_438L)
            )
        )
        assertEquals(mapOf(monday to 15_438L), days)
    }

    @Test
    fun `each day takes its fullest source`() {
        val days = StepSources.perDay(
            mapOf(
                "phone" to mapOf(sunday to 9_000L, monday to 4_000L),
                "watch" to mapOf(sunday to 8_500L, monday to 11_200L)
            )
        )
        assertEquals(mapOf(sunday to 9_000L, monday to 11_200L), days)
    }

    @Test
    fun `a day only one source has is kept`() {
        val days = StepSources.perDay(mapOf("phone" to mapOf(sunday to 700L), "watch" to emptyMap()))
        assertEquals(mapOf(sunday to 700L), days)
    }

    @Test
    fun `no sources, no days`() {
        assertEquals(emptyMap<LocalDate, Long>(), StepSources.perDay(emptyMap()))
    }
}
