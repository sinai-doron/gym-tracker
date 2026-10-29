package com.doron.gymtracker.data

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Decides which GPS fixes are good enough to keep and how much distance each adds.
 * Inaccurate fixes are dropped (they draw spikes on the map and inflate the distance),
 * and tiny moves are ignored so standing still doesn't accumulate GPS jitter.
 */
object RouteDistance {
    const val MAX_ACCURACY_M = 25f
    const val MIN_STEP_M = 3.0
    private const val EARTH_RADIUS_M = 6_371_000.0
    private const val SPEED_WINDOW_MS = 10_000L

    fun isUsable(point: LocationPoint): Boolean =
        point.accuracy == null || point.accuracy <= MAX_ACCURACY_M

    /** Meters to add when moving from [from] to [to]; 0 when the move is too small to trust. */
    fun step(from: LocationPoint, to: LocationPoint): Double {
        val d = metersBetween(from, to)
        return if (d >= MIN_STEP_M) d else 0.0
    }

    /** Distance of a whole recorded route, using the same rules as live recording. */
    fun total(points: List<LocationPoint>): Double {
        var anchor: LocationPoint? = null
        var sum = 0.0
        points.filter(::isUsable).forEach { p ->
            val a = anchor
            if (a == null) {
                anchor = p
            } else {
                val d = step(a, p)
                // Only move the anchor once the step counts, so slow walking still adds up
                if (d > 0) { sum += d; anchor = p }
            }
        }
        return sum
    }

    /**
     * Speed (m/s) over roughly the last [windowMs] of [route] ending at [latest], for when
     * the GPS fix itself carries no speed.
     */
    fun recentSpeed(route: List<LocationPoint>, latest: LocationPoint, windowMs: Long = SPEED_WINDOW_MS): Float {
        val from = route.lastOrNull { latest.timestamp - it.timestamp >= windowMs } ?: route.firstOrNull() ?: return 0f
        val secs = (latest.timestamp - from.timestamp) / 1000.0
        return if (secs > 0) (metersBetween(from, latest) / secs).toFloat() else 0f
    }

    fun metersBetween(a: LocationPoint, b: LocationPoint): Double {
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLng / 2).let { it * it }
        return 2 * EARTH_RADIUS_M * asin(sqrt(h))
    }
}
