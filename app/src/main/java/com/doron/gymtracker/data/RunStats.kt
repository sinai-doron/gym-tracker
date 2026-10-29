package com.doron.gymtracker.data

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

    fun gain(route: List<LocationPoint>): Double {
        val altitudes = smooth(route.mapNotNull { it.altitude })
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

    private fun smooth(values: List<Double>): List<Double> = values.indices.map { i ->
        val from = (i - SMOOTHING_WINDOW / 2).coerceAtLeast(0)
        val to = (i + SMOOTHING_WINDOW / 2).coerceAtMost(values.lastIndex)
        values.subList(from, to + 1).average()
    }
}

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
