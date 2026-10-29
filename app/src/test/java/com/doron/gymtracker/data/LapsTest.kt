package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LapsTest {

    private val start = 1_000_000L

    // A straight run north: one fix a second, [metersPerFix] apart (~111,195 m per degree)
    private fun run(fixes: Int, metersPerFix: Double, everyMs: Long = 1_000) = (1..fixes).map { i ->
        LocationPoint(32.0 + i * metersPerFix / 111_195.0, 34.8, null, null, 5f, start + i * everyMs)
    }

    @Test
    fun `cuts full laps and times them`() {
        // 1,200 m at 4 m/s: one full 500 m lap every 125 s, the rest is an unfinished lap
        val laps = Laps.split(start, run(300, 4.0), lapMeters = 500.0)
        assertEquals(2, laps.size)
        assertEquals(listOf(1, 2), laps.map { it.number })
        // The first lap also includes the second before the first fix
        assertEquals(126, laps[0].durationSecs, 1)
        assertEquals(125, laps[1].durationSecs, 1)
        assertEquals(250L, laps[1].paceSecsPerKm, 2)
    }

    @Test
    fun `a gap between fixes can finish several laps`() {
        // Two fixes 2.2 km apart, 10 minutes apart
        val laps = Laps.split(start, run(2, 2_200.0, everyMs = 600_000), lapMeters = 1_000.0)
        assertEquals(2, laps.size)
        assertTrue(laps[1].endedAt > laps[0].endedAt)
    }

    @Test
    fun `laps off means no laps`() {
        assertEquals(emptyList<Lap>(), Laps.split(start, run(300, 4.0), lapMeters = 0.0))
    }

    private fun assertEquals(expected: Long, actual: Long, tolerance: Long) =
        assertTrue("expected $expected±$tolerance but was $actual", kotlin.math.abs(expected - actual) <= tolerance)
}
