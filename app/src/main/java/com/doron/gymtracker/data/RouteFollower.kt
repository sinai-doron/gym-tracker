package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RoutePoint
import kotlin.math.cos
import kotlin.math.sqrt

/** Where the runner is on a planned route: how far along it, and how far off it. */
data class RoutePosition(val alongMeters: Double, val offMeters: Double, val segment: Int)

/**
 * Follows a runner along a planned route. Each fix is matched to the nearest part of the
 * route close ahead of where they last were, so on a loop (where the start and the end
 * are the same spot) setting off isn't mistaken for finishing. If nothing there matches,
 * the runner is off the route, even if some other part of it passes nearby; running on
 * that other part for a while counts as a shortcut and picks them up there.
 */
class RouteFollower(private val route: List<RoutePoint>) {
    /** Distance from the start of the route to each of its points. */
    private val cumulative: DoubleArray = DoubleArray(route.size).also { acc ->
        for (i in 1 until route.size) acc[i] = acc[i - 1] + meters(route[i - 1], route[i])
    }
    val lengthMeters: Double get() = cumulative.lastOrNull() ?: 0.0

    private var segment = 0
    // A part of the route away from where the runner was, that their last fixes have matched
    private var elsewhere: Int? = null
    private var elsewhereFixes = 0

    fun locate(latitude: Double, longitude: Double): RoutePosition? {
        if (route.size < 2) return null
        val near = best(latitude, longitude, (segment - BEHIND).coerceAtLeast(0), (segment + AHEAD).coerceAtMost(route.size - 2))
        if (near.offMeters <= ON_ROUTE_M) {
            // Only move on along the route; a fix that matches further back is GPS wobble
            segment = maxOf(segment, near.segment)
            elsewhere = null
            return near
        }
        // Off the part being run. Another part of the route may pass close by (a loop's way
        // back, a street run twice): only take that as a shortcut after running on it a while.
        val other = best(latitude, longitude, 0, route.size - 2)
        if (other.offMeters <= ON_ROUTE_M) {
            val same = elsewhere?.let { kotlin.math.abs(it - other.segment) <= AHEAD } ?: false
            elsewhereFixes = if (same) elsewhereFixes + 1 else 1
            elsewhere = other.segment
            if (elsewhereFixes >= SHORTCUT_FIXES) {
                segment = other.segment
                elsewhere = null
                return other
            }
        } else {
            elsewhere = null
        }
        return near
    }

    private fun best(lat: Double, lon: Double, from: Int, to: Int): RoutePosition {
        var best = RoutePosition(0.0, Double.MAX_VALUE, from)
        for (i in from..to) {
            val (fraction, off) = project(lat, lon, route[i], route[i + 1])
            if (off < best.offMeters) {
                best = RoutePosition(cumulative[i] + fraction * (cumulative[i + 1] - cumulative[i]), off, i)
            }
        }
        return best
    }

    /** How far along segment a→b the point is (0..1), and its distance from the segment, in meters. */
    private fun project(lat: Double, lon: Double, a: RoutePoint, b: RoutePoint): Pair<Double, Double> {
        // Flat-earth meters around a: plenty accurate over a route segment
        val kx = METERS_PER_DEGREE * cos(Math.toRadians(a.latitude))
        val bx = (b.longitude - a.longitude) * kx
        val by = (b.latitude - a.latitude) * METERS_PER_DEGREE
        val px = (lon - a.longitude) * kx
        val py = (lat - a.latitude) * METERS_PER_DEGREE
        val len2 = bx * bx + by * by
        val t = if (len2 == 0.0) 0.0 else ((px * bx + py * by) / len2).coerceIn(0.0, 1.0)
        val dx = px - t * bx
        val dy = py - t * by
        return t to sqrt(dx * dx + dy * dy)
    }

    companion object {
        /** Within this of the route counts as on it. */
        const val ON_ROUTE_M = 50.0
        private const val BEHIND = 3
        private const val AHEAD = 40
        // About 30 s of fixes on another part of the route makes it a shortcut, not a wrong turn
        private const val SHORTCUT_FIXES = 15
        private const val METERS_PER_DEGREE = 111_195.0

        fun meters(a: RoutePoint, b: RoutePoint): Double = RouteDistance.metersBetween(
            LocationPoint(a.latitude, a.longitude, null, null, null, 0),
            LocationPoint(b.latitude, b.longitude, null, null, null, 0)
        )

        fun length(points: List<RoutePoint>): Double = points.zipWithNext { a, b -> meters(a, b) }.sum()
    }
}
