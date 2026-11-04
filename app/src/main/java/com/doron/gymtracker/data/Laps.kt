package com.doron.gymtracker.data

import kotlin.math.roundToLong

/** One lap of a run. [endedAt] is when the lap's distance was reached. */
data class Lap(
    val number: Int,
    val distanceMeters: Double,
    val durationSecs: Long,
    val endedAt: Long,
    /** The last stretch of a finished run, shorter than a full lap. */
    val partial: Boolean = false
) {
    /** Seconds per kilometer. */
    val paceSecsPerKm: Long
        get() = (durationSecs * 1000.0 / distanceMeters).roundToLong()
}

object Laps {
    /**
     * The full laps of [lapMeters] along [route], timed from [startedAt]. Each lap ends at
     * the moment its distance was crossed, interpolated between the two fixes around it.
     * With a [clock], lap times leave out auto-paused time. For a run [finishedAt] a moment,
     * whatever was run after the last full lap is added as a [Lap.partial] one.
     */
    fun split(
        startedAt: Long,
        route: List<LocationPoint>,
        lapMeters: Double,
        clock: ActiveClock? = null,
        finishedAt: Long? = null,
        pauses: List<Pause> = emptyList()
    ): List<Lap> {
        if (lapMeters <= 0) return emptyList()
        val laps = mutableListOf<Lap>()
        var lapStart = startedAt
        var previous: DistanceMark? = null
        val marks = RouteDistance.marks(route, pauses)
        for (mark in marks) {
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
        val rest = (marks.lastOrNull()?.meters ?: 0.0) - laps.size * lapMeters
        if (finishedAt != null && rest >= MIN_PARTIAL_M) {
            val lapMs = clock?.let { it.activeMs(finishedAt) - it.activeMs(lapStart) } ?: (finishedAt - lapStart)
            laps += Lap(laps.size + 1, rest, (lapMs / 1000.0).roundToLong(), finishedAt, partial = true)
        }
        return laps
    }

    /**
     * Each lap's climb, in [laps] order: the climb ([Elevation.climb]) of the fixes from the lap
     * before's end to this one's, with the fix on either side so no rise between laps is lost.
     * Null when the route has no altitudes.
     */
    fun climbs(laps: List<Lap>, route: List<LocationPoint>): List<Double>? {
        if (route.none { it.altitude != null }) return null
        var from = Long.MIN_VALUE
        return laps.map { lap ->
            val first = route.indexOfFirst { it.timestamp >= from }.let { if (it > 0) it - 1 else 0 }
            val last = route.indexOfLast { it.timestamp <= lap.endedAt }.let { (it + 1).coerceAtMost(route.lastIndex) }
            from = lap.endedAt
            if (last <= first) 0.0 else Elevation.climb(route.subList(first, last + 1).mapNotNull { it.altitude })
        }
    }

    /** A last stretch shorter than this isn't worth a row of its own. */
    private const val MIN_PARTIAL_M = 10.0
}
