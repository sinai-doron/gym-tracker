package com.doron.gymtracker.data

/** A pause the runner asked for. [endedAt] is null while it lasts. */
data class Pause(val startedAt: Long, val endedAt: Long? = null) {
    fun contains(t: Long) = t >= startedAt && (endedAt == null || t < endedAt)
}

/**
 * Moving time with auto-pause: a run pauses once it hasn't moved for [pauseAfterMs] and
 * resumes with the next move. "Moves" are the fixes that counted towards the distance, so
 * GPS jitter while standing still doesn't resume it. The time before a pause kicks in
 * still counts, as a run can't know it stopped until then.
 *
 * A gap between moves that covered ground at [MOVING_SPEED_MPS] or more wasn't a stop but
 * lost GPS (under a bridge, between tall buildings): it counts as moving once the fixes
 * come back, though [isPaused] can't know that while they're missing.
 *
 * [manualPauses] stop the clock from the moment they're asked for until resumed, whether
 * or not the runner is moving.
 */
class ActiveClock(
    private val startedAt: Long,
    /** Each move: when it happened and the distance covered by then, in order. */
    moves: List<DistanceMark>,
    private val manualPauses: List<Pause> = emptyList(),
    private val pauseAfterMs: Long = PAUSE_AFTER_MS
) {
    private val moves = listOf(DistanceMark(startedAt, moves.firstOrNull()?.meters ?: 0.0)) +
        moves.filter { it.timestamp >= startedAt }

    private val autoPauses: List<Pair<Long, Long>> = this.moves.zipWithNext()
        .filter { (a, b) -> b.timestamp - a.timestamp > pauseAfterMs && !keptMoving(a, b) }
        .map { (a, b) -> (a.timestamp + pauseAfterMs) to b.timestamp }

    /** Finished paused stretches, automatic and manual merged, as (pausedAt, resumedAt). */
    val pauses: List<Pair<Long, Long>> =
        merge(autoPauses + manualPauses.mapNotNull { p -> p.endedAt?.let { p.startedAt to it } })

    private fun keptMoving(a: DistanceMark, b: DistanceMark): Boolean =
        (b.meters - a.meters) / ((b.timestamp - a.timestamp) / 1000.0) >= MOVING_SPEED_MPS

    /** Paused by hand right now. */
    fun isManuallyPaused(now: Long): Boolean = manualPauses.any { it.contains(now) }

    fun isPaused(now: Long): Boolean =
        isManuallyPaused(now) ||
            now - (moves.lastOrNull { it.timestamp <= now }?.timestamp ?: startedAt) > pauseAfterMs

    /** Moving time from the start until [at]. */
    fun activeMs(at: Long): Long {
        // A fix can carry a time from just before the run started
        if (at <= startedAt) return 0
        val last = moves.last().timestamp
        val stretches = autoPauses +
            // Not moved since the last fix: paused from when auto-pause would have kicked in
            listOfNotNull(if (at > last + pauseAfterMs) (last + pauseAfterMs) to at else null) +
            manualPauses.map { it.startedAt to (it.endedAt ?: at) }
        val paused = merge(stretches.map { (a, b) -> a.coerceIn(startedAt, at) to b.coerceIn(startedAt, at) })
            .sumOf { (a, b) -> b - a }
        return (at - startedAt - paused).coerceAtLeast(0)
    }

    companion object {
        const val PAUSE_AFTER_MS = 10_000L
        /** A slow walk; standing at a light while GPS drifts is far below it. */
        const val MOVING_SPEED_MPS = 1.0

        fun forRoute(startedAt: Long, route: List<LocationPoint>, manualPauses: List<Pause> = emptyList()) =
            ActiveClock(startedAt, RouteDistance.marks(route, manualPauses), manualPauses)

        /** Overlapping or touching stretches joined, so time paused for two reasons counts once. */
        private fun merge(stretches: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
            val merged = mutableListOf<Pair<Long, Long>>()
            for ((a, b) in stretches.filter { it.second > it.first }.sortedBy { it.first }) {
                val prev = merged.lastOrNull()
                if (prev != null && a <= prev.second) merged[merged.lastIndex] = prev.first to maxOf(prev.second, b)
                else merged += a to b
            }
            return merged
        }
    }
}
