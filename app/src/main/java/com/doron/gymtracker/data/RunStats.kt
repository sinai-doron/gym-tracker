package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RoutePoint
import kotlin.math.roundToInt

/** Altitude at a distance along the route, for the elevation chart. */
data class ElevationPoint(val meters: Double, val altitudeM: Double)

/**
 * Climb from GPS altitude. Raw GPS altitude wanders by several meters fix to fix, so it's
 * smoothed first, and only rises of at least [CLIMB_THRESHOLD_M] count, or standing on a
 * flat street would slowly "climb".
 */
object Elevation {
    const val CLIMB_THRESHOLD_M = 3.0
    private const val SMOOTHING_WINDOW = 3

    fun gain(route: List<LocationPoint>): Double = climb(route.mapNotNull { it.altitude })

    /** Total climb over a series of altitudes, in order. */
    fun climb(rawAltitudes: List<Double>): Double {
        val altitudes = smooth(rawAltitudes)
        if (altitudes.isEmpty()) return 0.0
        // Each climb counts in full, low point to top, once it's over (a drop of the threshold
        // from the top) or the route ends; wobbles smaller than the threshold never count
        var low = altitudes.first()
        var high = low
        var gain = 0.0
        for (a in altitudes) {
            when {
                a > high -> high = a
                a <= high - CLIMB_THRESHOLD_M -> {
                    if (high - low >= CLIMB_THRESHOLD_M) gain += high - low
                    low = a
                    high = a
                }
                a < low -> { low = a; high = a }
            }
        }
        if (high - low >= CLIMB_THRESHOLD_M) gain += high - low
        return gain
    }

    /** Smoothed altitude against distance covered, at each fix that counted towards the distance. */
    fun profile(route: List<LocationPoint>): List<ElevationPoint> {
        val withAltitude = route.filter { it.altitude != null }
        val smoothed = smooth(withAltitude.map { it.altitude!! })
        val altitudeAt = withAltitude.map { it.timestamp }.zip(smoothed).toMap()
        return RouteDistance.marks(route).mapNotNull { mark ->
            altitudeAt[mark.timestamp]?.let { ElevationPoint(mark.meters, it) }
        }
    }

    /** Total descent: the climb of the same altitudes walked backwards. */
    fun descent(rawAltitudes: List<Double>): Double = climb(rawAltitudes.reversed())

    /** A planned route's altitude against distance along it, or empty if it has no altitudes. */
    fun routeProfile(points: List<RoutePoint>): List<ElevationPoint> {
        if (points.size < 2 || points.any { it.elevationM == null }) return emptyList()
        val smoothed = smooth(points.map { it.elevationM!! })
        return alongRoute(points).zip(smoothed) { m, a -> ElevationPoint(m, a) }
    }

    /**
     * Each segment of a planned route marked as uphill, downhill or flat, consecutive alike
     * segments joined. The grade is taken over [GRADE_SPAN_M] either side, since GPS altitude
     * between two points 10 m apart is mostly noise. Empty if the route has no altitudes.
     */
    fun slopes(points: List<RoutePoint>): List<Pair<Slope, List<RoutePoint>>> {
        val profile = routeProfile(points)
        if (profile.isEmpty()) return emptyList()
        val d = profile.map { it.meters }
        val slopeOf = (0 until points.lastIndex).map { i ->
            val from = d.indexOfLast { it <= d[i] - GRADE_SPAN_M }.coerceAtLeast(0)
            val to = d.indexOfFirst { it >= d[i + 1] + GRADE_SPAN_M }.let { if (it < 0) d.lastIndex else it }
            val grade = (profile[to].altitudeM - profile[from].altitudeM) / (d[to] - d[from]).coerceAtLeast(1.0)
            when {
                grade >= SLOPE_GRADE -> Slope.UP
                grade <= -SLOPE_GRADE -> Slope.DOWN
                else -> Slope.FLAT
            }
        }
        val runs = mutableListOf<Pair<Slope, MutableList<RoutePoint>>>()
        slopeOf.forEachIndexed { i, slope ->
            if (runs.lastOrNull()?.first == slope) runs.last().second += points[i + 1]
            else runs += slope to mutableListOf(points[i], points[i + 1])
        }
        return runs
    }

    private fun alongRoute(points: List<RoutePoint>): List<Double> =
        points.runningFoldIndexed(0.0) { i, acc, p -> if (i == 0) acc else acc + RouteFollower.meters(points[i - 1], p) }
            .drop(1)

    private const val GRADE_SPAN_M = 30.0
    private const val SLOPE_GRADE = 0.03

    private fun smooth(values: List<Double>): List<Double> = values.indices.map { i ->
        val from = (i - SMOOTHING_WINDOW / 2).coerceAtLeast(0)
        val to = (i + SMOOTHING_WINDOW / 2).coerceAtMost(values.lastIndex)
        values.subList(from, to + 1).average()
    }
}

enum class Slope { UP, DOWN, FLAT }

/** Calories from distance and body weight: the energy cost per kg per km barely depends on pace. */
object RunCalories {
    private const val RUNNING_KCAL_PER_KG_KM = 1.036
    private const val WALKING_KCAL_PER_KG_KM = 0.53

    fun estimate(type: String, distanceMeters: Double, bodyWeightKg: Float): Int {
        val perKgKm = if (type == "WALKING") WALKING_KCAL_PER_KG_KM else RUNNING_KCAL_PER_KG_KM
        return (perKgKm * bodyWeightKg * distanceMeters / 1000.0).roundToInt()
    }
}

/** The phone's step counter at a moment. */
data class StepSample(val timestamp: Long, val counter: Long)

/** Steps per minute. */
object Cadence {
    const val WINDOW_MS = 30_000L

    /** Over the last [WINDOW_MS], or null without two samples far enough apart. */
    fun current(samples: List<StepSample>, now: Long): Int? {
        val recent = samples.filter { now - it.timestamp <= WINDOW_MS }
        val first = recent.firstOrNull() ?: return null
        val last = recent.last()
        val secs = (last.timestamp - first.timestamp) / 1000.0
        if (secs < MIN_SPAN_SECS) return null
        return ((last.counter - first.counter) * 60 / secs).roundToInt()
    }

    fun average(steps: Int, elapsedSecs: Long): Int? =
        if (elapsedSecs >= 60) (steps * 60.0 / elapsedSecs).roundToInt() else null

    private const val MIN_SPAN_SECS = 10.0
}
