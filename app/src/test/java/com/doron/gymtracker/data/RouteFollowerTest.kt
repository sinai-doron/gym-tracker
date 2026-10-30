package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RoutePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteFollowerTest {

    // 1.2 km square loop (300 m sides), a point every 10 m like a saved route, ending where it starts
    private val d = 100 / 111_195.0
    private val step = d / 10
    private val square: List<RoutePoint> = buildList {
        for (i in 0 until 30) add(RoutePoint(32.0 + i * step, 34.8))
        for (i in 0 until 30) add(RoutePoint(32.0 + 3 * d, 34.8 + i * step * 1.18))
        for (i in 0 until 30) add(RoutePoint(32.0 + 3 * d - i * step, 34.8 + 3 * d * 1.18))
        for (i in 0..30) add(RoutePoint(32.0, 34.8 + 3 * d * 1.18 - i * step * 1.18))
    }

    @Test
    fun `measures the route`() {
        assertEquals(1_200.0, RouteFollower(square).lengthMeters, 15.0)
    }

    @Test
    fun `setting off on a loop is the start, not the finish`() {
        val follower = RouteFollower(square)
        val start = follower.locate(32.0 + 10 / 111_195.0, 34.8)!!
        assertEquals(10.0, start.alongMeters, 3.0)
    }

    @Test
    fun `follows the runner round and back to the start`() {
        val follower = RouteFollower(square)
        square.forEach { follower.locate(it.latitude, it.longitude) }
        val finish = follower.locate(32.0, 34.8)!!
        assertEquals(follower.lengthMeters, finish.alongMeters, 15.0)
    }

    @Test
    fun `knows when the runner is off the route`() {
        val follower = RouteFollower(square)
        follower.locate(32.0, 34.8)
        // 150 m west of the first side
        val off = follower.locate(32.0 + d, 34.8 - 150 / (111_195.0 * 0.848))!!
        assertTrue("off by ${off.offMeters}", off.offMeters > 120)
    }

    @Test
    fun `passing near the way back isn't finishing the loop`() {
        val follower = RouteFollower(square)
        follower.locate(32.0, 34.8)
        follower.locate(32.0 + d, 34.8)
        // A detour onto the loop's last side (the bottom edge), 2 fixes: off the route, not finished
        val detour = (1..2).map { follower.locate(32.0, 34.8 + d)!! }
        assertTrue("off by ${detour.last().offMeters}", detour.last().offMeters > RouteFollower.ON_ROUTE_M)
        assertTrue("along ${detour.last().alongMeters}", detour.last().alongMeters < 200)
    }

    @Test
    fun `running on another part of the route a while is a shortcut`() {
        val follower = RouteFollower(square)
        follower.locate(32.0, 34.8)
        // Cut straight across to the far side and keep running there
        val positions = (0 until 16).map { follower.locate(32.0 + (3 - it * 0.1) * d, 34.8 + 3 * d * 1.18)!! }
        assertTrue("along ${positions.last().alongMeters}", positions.last().alongMeters > 600)
        assertTrue(positions.last().offMeters <= RouteFollower.ON_ROUTE_M)
    }

    @Test
    fun `reads a GPX track`() {
        val gpx = """<?xml version="1.0"?><gpx><metadata><name>File</name></metadata>
            <trk><name>Morning loop</name><trkseg>
            <trkpt lat="51.5" lon="-0.13"><ele>20</ele></trkpt>
            <trkpt lon="-0.131" lat="51.501"/>
            </trkseg></trk></gpx>"""
        val parsed = Gpx.parse(gpx)
        assertEquals("Morning loop", parsed.name)
        assertEquals(listOf(RoutePoint(51.5, -0.13, 20.0), RoutePoint(51.501, -0.131, null)), parsed.points)
    }

    @Test
    fun `reads the London GPX`() {
        val file = java.io.File(System.getProperty("user.home"), "Downloads/sanderson-loop.gpx")
        if (!file.exists()) return
        val parsed = Gpx.parse(file.readText())
        assertEquals("Sanderson Sightseeing Loop", parsed.name)
        assertEquals(186, parsed.points.size)
        assertEquals(5_015.0, RouteFollower.length(parsed.points), 10.0)
    }
}
