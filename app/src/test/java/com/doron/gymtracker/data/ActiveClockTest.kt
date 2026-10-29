package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveClockTest {

    // Moving every 2 s for a minute
    private val steady = (1..30).map { it * 2_000L }

    @Test
    fun `moving all along counts every second`() {
        val clock = ActiveClock(0, steady)
        assertEquals(60_000, clock.activeMs(60_000))
        assertFalse(clock.isPaused(60_000))
    }

    @Test
    fun `a stop pauses after 10 s and resumes with the next move`() {
        // Stopped from 60 s to 100 s (traffic light), then moving again
        val clock = ActiveClock(0, steady + listOf(100_000L, 102_000L))
        assertTrue(clock.isPaused(80_000))
        assertEquals(70_000, clock.activeMs(80_000))   // paused at 70 s
        assertEquals(70_000, clock.activeMs(100_000))  // still paused until the move at 100 s
        assertEquals(72_000, clock.activeMs(102_000))
        assertEquals(listOf(70_000L to 100_000L), clock.pauses)
    }

    @Test
    fun `standing still at the start pauses too`() {
        val clock = ActiveClock(0, listOf(30_000L, 32_000L))
        assertEquals(12_000, clock.activeMs(32_000))
    }

    @Test
    fun `a short stop doesn't pause`() {
        val clock = ActiveClock(0, listOf(2_000L, 10_000L, 12_000L))
        assertEquals(12_000, clock.activeMs(12_000))
    }
}
