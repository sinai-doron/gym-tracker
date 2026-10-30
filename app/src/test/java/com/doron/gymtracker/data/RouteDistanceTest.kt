package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteDistanceTest {

    // ~1.11 m per 0.00001° of latitude
    private fun point(latOffset: Double, accuracy: Float? = 5f) =
        LocationPoint(32.0 + latOffset, 34.8, speed = null, altitude = null, accuracy = accuracy, timestamp = 0)

    @Test
    fun `measures a straight line`() {
        assertEquals(111.2, RouteDistance.metersBetween(point(0.0), point(0.001)), 0.5)
    }

    @Test
    fun `drops inaccurate fixes`() {
        assertTrue(RouteDistance.isUsable(point(0.0, accuracy = 10f)))
        assertFalse(RouteDistance.isUsable(point(0.0, accuracy = 80f)))
        // A wild fix in the middle of a 100 m walk adds nothing
        val route = listOf(point(0.0), point(0.01, accuracy = 80f), point(0.0009))
        assertEquals(100.0, RouteDistance.total(route), 1.0)
    }

    @Test
    fun `standing still adds no jitter`() {
        val jitter = (0 until 50).map { point(if (it % 2 == 0) 0.0 else 0.00001) }
        assertEquals(0.0, RouteDistance.total(jitter), 0.0)
    }

    @Test
    fun `slow walking still adds up`() {
        // 100 fixes 1.1 m apart: each alone is under the threshold, together 110 m
        val walk = (0..100).map { point(it * 0.00001) }
        assertEquals(111.0, RouteDistance.total(walk), 4.0)
    }

    @Test
    fun `works out speed from the route when GPS gives none`() {
        // 111 m in 30 s, one fix every second: 3.7 m/s
        val route = (0..30).map { point(it * 0.0000333).copy(timestamp = it * 1000L) }
        assertEquals(3.7f, RouteDistance.recentSpeed(route.dropLast(1), route.last()), 0.1f)
    }

    @Test
    fun `the way walked during a pause isn't distance`() {
        // 100 m north, a pause from 100 s to 400 s, then 100 m more from 300 m further on
        fun at(meters: Double, t: Long) =
            LocationPoint(32.0 + meters / 111_195.0, 34.8, null, null, 5f, t)
        val route = (0..10).map { at(it * 10.0, it * 10_000L) } +
            (0..10).map { at(400 + it * 10.0, 400_000L + it * 10_000L) }
        val pause = Pause(100_001L, 400_000L)
        assertEquals(500.0, RouteDistance.total(route), 1.0)
        assertEquals(200.0, RouteDistance.total(route, listOf(pause)), 1.0)
    }
}
