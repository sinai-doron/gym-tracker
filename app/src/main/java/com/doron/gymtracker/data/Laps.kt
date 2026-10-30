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

    /** A last stretch shorter than this isn't worth a row of its own. */
    private const val MIN_PARTIAL_M = 10.0
}
