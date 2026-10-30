package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunStatsTest {

    // Fixes 10 m apart heading north, one a second
    private fun route(altitudes: List<Double>) = altitudes.mapIndexed { i, alt ->
        LocationPoint(32.0 + i * 10 / 111_195.0, 34.8, null, alt, 5f, i * 1_000L)
    }

    @Test
    fun `counts a real climb`() {
        // 20 m up a hill, then back down (smoothing rounds off the very bottom and top a little)
        val hill = (0..20).map { it.toDouble() } + (19 downTo 0).map { it.toDouble() }
        assertEquals(20.0, Elevation.gain(route(hill)), 2.5)
    }

    @Test
    fun `adds up separate climbs`() {
        val twoHills = List(2) { (0..10).map { it.toDouble() } + (9 downTo 0).map { it.toDouble() } }.flatten()
        assertEquals(20.0, Elevation.gain(route(twoHills)), 4.0)
    }

    @Test
    fun `GPS altitude noise on a flat street is not a climb`() {
        val noisy = (0..120).map { 50.0 + if (it % 2 == 0) 1.5 else -1.5 }
        assertEquals(0.0, Elevation.gain(route(noisy)), 0.0)
    }

    // Planned-route points 10 m apart heading north
    private fun planned(altitudes: List<Double?>) = altitudes.mapIndexed { i, alt ->
        com.doron.gymtracker.domain.model.RoutePoint(32.0 + i * 10 / 111_195.0, 34.8, alt)
    }

    @Test
    fun `descent mirrors the climb`() {
        val upThenDown = (0..30).map { it.toDouble() } + (29 downTo 10).map { it.toDouble() }
        assertEquals(30.0, Elevation.climb(upThenDown), 2.5)
        assertEquals(20.0, Elevation.descent(upThenDown), 2.5)
    }

    @Test
    fun `route profile needs every altitude`() {
        assertTrue(Elevation.routeProfile(planned(listOf(10.0, null, 12.0))).isEmpty())
        val profile = Elevation.routeProfile(planned(listOf(10.0, 10.0, 10.0)))
        assertEquals(listOf(0.0, 10.0, 20.0), profile.map { Math.round(it.meters).toDouble() })
    }

    @Test
    fun `slopes split a hill into up, flat and down`() {
        // 300 m up at 10%, 300 m flat, 300 m down at 10%
        val alts = (0..30).map { it.toDouble() } + List(30) { 30.0 } + (29 downTo 0).map { it.toDouble() }
        val slopes = Elevation.slopes(planned(alts)).map { it.first }
        assertEquals(listOf(Slope.UP, Slope.FLAT, Slope.DOWN), slopes)
        // Joined runs share their end points, so the line has no gaps
        val runs = Elevation.slopes(planned(alts))
        runs.zipWithNext().forEach { (a, b) -> assertEquals(a.second.last(), b.second.first()) }
    }

    @Test
    fun `no slopes without altitudes`() {
        assertTrue(Elevation.slopes(planned(listOf(null, null, null))).isEmpty())
    }

    @Test
    fun `profile follows the distance`() {
        val profile = Elevation.profile(route((0..10).map { it.toDouble() }))
        assertEquals(11, profile.size)
        assertEquals(100.0, profile.last().meters, 1.0)
        assertTrue(profile.last().altitudeM > profile.first().altitudeM)
    }

    @Test
    fun `calories scale with distance and weight`() {
        assertEquals(414, RunCalories.estimate("RUNNING", 5_000.0, 80f))
        assertEquals(212, RunCalories.estimate("WALKING", 5_000.0, 80f))
    }

    @Test
    fun `cadence from the step counter`() {
        // 90 steps in 30 s = 180 steps a minute
        val samples = listOf(StepSample(0, 1_000), StepSample(15_000, 1_045), StepSample(30_000, 1_090))
        assertEquals(180, Cadence.current(samples, now = 30_000))
        assertNull(Cadence.current(samples.take(1), now = 1_000))
        assertEquals(170, Cadence.average(steps = 1_700, elapsedSecs = 600))
    }
}
