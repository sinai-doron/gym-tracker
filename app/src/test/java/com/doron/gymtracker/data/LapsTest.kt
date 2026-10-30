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
    fun `a finished run ends with the part of a lap after the last full one`() {
        // 1,200 m at 4 m/s, finished 5 s after the last fix
        val route = run(300, 4.0)
        val laps = Laps.split(start, route, lapMeters = 500.0, finishedAt = route.last().timestamp + 5_000)
        assertEquals(3, laps.size)
        assertTrue(laps.last().partial)
        assertTrue(laps.dropLast(1).none { it.partial })
        assertEquals(196.0, laps.last().distanceMeters, 1.0)
        // 49 s to the last fix, then the 5 s to finishing
        assertEquals(54, laps.last().durationSecs, 1)
    }

    @Test
    fun `a run still going has only full laps`() {
        assertTrue(Laps.split(start, run(300, 4.0), lapMeters = 500.0).none { it.partial })
    }

    @Test
    fun `a few meters past the last lap don't make a lap`() {
        // 1,004 m: two full laps and 4 m
        val route = run(251, 4.0)
        assertEquals(2, Laps.split(start, route, lapMeters = 500.0, finishedAt = route.last().timestamp).size)
    }

    @Test
    fun `laps off means no laps`() {
        assertEquals(emptyList<Lap>(), Laps.split(start, run(300, 4.0), lapMeters = 0.0))
    }

    private fun assertEquals(expected: Double, actual: Double, tolerance: Double) =
        org.junit.Assert.assertEquals(expected, actual, tolerance)

    private fun assertEquals(expected: Long, actual: Long, tolerance: Long) =
        assertTrue("expected $expected±$tolerance but was $actual", kotlin.math.abs(expected - actual) <= tolerance)
}
