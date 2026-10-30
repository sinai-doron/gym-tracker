package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveClockTest {

    // Moves at 3 m/s
    private fun running(times: List<Long>) = times.map { DistanceMark(it, it * 3 / 1000.0) }
    // Moving every 2 s for a minute
    private val steady = running((1..30).map { it * 2_000L })
    // A step or two on from where the run got to
    private fun stepAfter(prev: DistanceMark, at: Long) = DistanceMark(at, prev.meters + 3)

    @Test
    fun `moving all along counts every second`() {
        val clock = ActiveClock(0, steady)
        assertEquals(60_000, clock.activeMs(60_000))
        assertFalse(clock.isPaused(60_000))
    }

    @Test
    fun `a stop pauses after 10 s and resumes with the next move`() {
        // Stopped from 60 s to 100 s (traffic light), then moving again
        val resumed = stepAfter(steady.last(), 100_000L)
        val clock = ActiveClock(0, steady + resumed + stepAfter(resumed, 102_000L))
        assertTrue(clock.isPaused(80_000))
        assertEquals(70_000, clock.activeMs(80_000))   // paused at 70 s
        assertEquals(70_000, clock.activeMs(100_000))  // still paused until the move at 100 s
        assertEquals(72_000, clock.activeMs(102_000))
        assertEquals(listOf(70_000L to 100_000L), clock.pauses)
    }

    @Test
    fun `standing still at the start pauses too`() {
        val clock = ActiveClock(0, listOf(DistanceMark(30_000L, 3.0), DistanceMark(32_000L, 6.0)))
        assertEquals(12_000, clock.activeMs(32_000))
    }

    @Test
    fun `a short stop doesn't pause`() {
        val clock = ActiveClock(0, running(listOf(2_000L, 10_000L, 12_000L)))
        assertEquals(12_000, clock.activeMs(12_000))
    }

    @Test
    fun `lost GPS while running isn't a stop`() {
        // No fixes from 60 s to 100 s, but 120 m further on when they come back
        val back = DistanceMark(100_000L, steady.last().meters + 120)
        val clock = ActiveClock(0, steady + back)
        assertEquals(100_000, clock.activeMs(100_000))
        assertEquals(emptyList<Pair<Long, Long>>(), clock.pauses)
    }

    @Test
    fun `a pause by hand stops the clock even while moving`() {
        val clock = ActiveClock(0, steady, listOf(Pause(20_000L, 30_000L)))
        assertEquals(50_000, clock.activeMs(60_000))
        assertTrue(clock.isPaused(25_000))
        assertFalse(clock.isPaused(31_000))
    }

    @Test
    fun `an open pause lasts until resumed`() {
        val clock = ActiveClock(0, steady, listOf(Pause(40_000L)))
        assertTrue(clock.isManuallyPaused(500_000))
        assertEquals(40_000, clock.activeMs(500_000))
    }

    @Test
    fun `paused by hand and by standing still counts once`() {
        // Stood still from 60 s to 100 s (auto-pause from 70 s) and paused by hand from 65 s to 90 s
        val resumed = stepAfter(steady.last(), 100_000L)
        val clock = ActiveClock(0, steady + resumed, listOf(Pause(65_000L, 90_000L)))
        assertEquals(65_000, clock.activeMs(100_000))
        assertEquals(listOf(65_000L to 100_000L), clock.pauses)
    }

    @Test
    fun `no time before the run started`() {
        val clock = ActiveClock(10_000, running(listOf(12_000L, 14_000L)))
        assertEquals(0, clock.activeMs(9_300))
    }
}
