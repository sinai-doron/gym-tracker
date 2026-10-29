package com.doron.gymtracker.data

import kotlin.math.roundToLong

/** One completed lap of a run. [endedAt] is when the lap's distance was reached. */
data class Lap(
    val number: Int,
    val distanceMeters: Double,
    val durationSecs: Long,
    val endedAt: Long
) {
    /** Seconds per kilometer. */
    val paceSecsPerKm: Long
        get() = (durationSecs * 1000.0 / distanceMeters).roundToLong()
}

object Laps {
    /**
     * The full laps of [lapMeters] along [route], timed from [startedAt]. Each lap ends at
     * the moment its distance was crossed, interpolated between the two fixes around it.
     * With a [clock], lap times leave out auto-paused time.
     */
    fun split(startedAt: Long, route: List<LocationPoint>, lapMeters: Double, clock: ActiveClock? = null): List<Lap> {
        if (lapMeters <= 0) return emptyList()
        val laps = mutableListOf<Lap>()
        var lapStart = startedAt
        var previous: DistanceMark? = null
        for (mark in RouteDistance.marks(route)) {
            val prev = previous
            previous = mark
            if (prev == null) continue
            // A long gap between fixes can cross more than one lap boundary
            while (mark.meters >= (laps.size + 1) * lapMeters) {
                val boundary = (laps.size + 1) * lapMeters
                val fraction = (boundary - prev.meters) / (mark.meters - prev.meters)
                val endedAt = prev.timestamp + ((mark.timestamp - prev.timestamp) * fraction).roundToLong()
                val lapMs = clock?.let { it.activeMs(endedAt) - it.activeMs(lapStart) } ?: (endedAt - lapStart)
                laps += Lap(laps.size + 1, lapMeters, (lapMs / 1000.0).roundToLong(), endedAt)
                lapStart = endedAt
            }
        }
        return laps
    }
}
