package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenceTest {

    private val start = 1_000_000L
    // 1 km in 10 minutes, steadily
    private val marks = (0..10).map { DistanceMark(start + it * 60_000L, it * 100.0) }

    /** The run's steps so far every 10 s, at [spm] steps a minute throughout. */
    private fun steady(minutes: Int, spm: Int) = (0..minutes * 6).map { i -> StepMark(start + i * 10_000L, spm * i / 6) }

    @Test
    fun `a run recorded before steps were saved as it went has no cadence over the run`() {
        assertTrue(Cadence.profile(emptyList(), start, marks, emptyList()).isEmpty())
        assertNull(Cadence.laps(listOf(Lap(1, 1000.0, 600, start + 600_000)), emptyList(), start))
    }

    @Test
    fun `a point a minute, placed at how far the run had gone`() {
        val profile = Cadence.profile(steady(minutes = 3, spm = 168), start, marks, emptyList())
        assertEquals(listOf(100.0, 200.0, 300.0), profile.map { it.first })
        profile.forEach { assertEquals(168.0, it.second, 0.5) }
    }

    @Test
    fun `a pause the runner asked for and a minute stood still are left out`() {
        // Running, then a 2-minute pause (walking about: 50 steps), then a minute stood at a light
        val running = steady(minutes = 2, spm = 170)
        val afterPause = StepMark(start + 240_000, running.last().steps + 50)
        val standing = (1..6).map { StepMark(afterPause.timestamp + it * 10_000L, afterPause.steps + it) }
        val pause = Pause(start + 120_000, start + 240_000)
        val profile = Cadence.profile(running + afterPause + standing, start, marks, listOf(pause))
        assertEquals(2, profile.size)
        profile.forEach { assertEquals(170.0, it.second, 0.5) }
    }

    @Test
    fun `each lap's cadence is its steps over its moving time`() {
        // 160 a minute for 5 minutes, then 180 a minute for 5: laps of 500 m in 5 minutes each
        val first = steady(minutes = 5, spm = 160)
        val second = (1..30).map { i -> StepMark(first.last().timestamp + i * 10_000L, first.last().steps + 180 * i / 6) }
        val laps = listOf(Lap(1, 500.0, 300, start + 300_000), Lap(2, 500.0, 300, start + 600_000))
        assertEquals(listOf(160, 180), Cadence.laps(laps, first + second, start))
    }

    @Test
    fun `a lap with an unbelievable figure shows none`() {
        val laps = listOf(Lap(1, 500.0, 300, start + 300_000), Lap(2, 20.0, 0, start + 310_000, partial = true))
        assertEquals(listOf(160, null), Cadence.laps(laps, steady(minutes = 6, spm = 160), start))
    }
}
