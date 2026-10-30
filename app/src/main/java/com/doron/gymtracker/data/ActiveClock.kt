package com.doron.gymtracker.data

/**
 * Moving time with auto-pause: a run pauses once it hasn't moved for [pauseAfterMs] and
 * resumes with the next move. "Moves" are the fixes that counted towards the distance, so
 * GPS jitter while standing still doesn't resume it. The time before a pause kicks in
 * still counts, as a run can't know it stopped until then.
 *
 * A gap between moves that covered ground at [MOVING_SPEED_MPS] or more wasn't a stop but
 * lost GPS (under a bridge, between tall buildings): it counts as moving once the fixes
 * come back, though [isPaused] can't know that while they're missing.
 */
class ActiveClock(
    private val startedAt: Long,
    /** Each move: when it happened and the distance covered by then, in order. */
    moves: List<DistanceMark>,
    private val pauseAfterMs: Long = PAUSE_AFTER_MS
) {
    private val moves = listOf(DistanceMark(startedAt, moves.firstOrNull()?.meters ?: 0.0)) +
        moves.filter { it.timestamp >= startedAt }

    /** Paused stretches between moves, as (pausedAt, resumedAt). */
    val pauses: List<Pair<Long, Long>> = this.moves.zipWithNext()
        .filter { (a, b) -> b.timestamp - a.timestamp > pauseAfterMs && !keptMoving(a, b) }
        .map { (a, b) -> (a.timestamp + pauseAfterMs) to b.timestamp }

    private fun keptMoving(a: DistanceMark, b: DistanceMark): Boolean =
        (b.meters - a.meters) / ((b.timestamp - a.timestamp) / 1000.0) >= MOVING_SPEED_MPS

    fun isPaused(now: Long): Boolean =
        now - (moves.lastOrNull { it.timestamp <= now }?.timestamp ?: startedAt) > pauseAfterMs

    /** Moving time from the start until [at]. */
    fun activeMs(at: Long): Long {
        var paused = pauses.sumOf { (from, to) -> (minOf(to, at) - from).coerceAtLeast(0) }
        val last = moves.last().timestamp
        if (at > last + pauseAfterMs) paused += at - (last + pauseAfterMs)
        return (at - startedAt - paused).coerceAtLeast(0)
    }

    companion object {
        const val PAUSE_AFTER_MS = 10_000L
        /** A slow walk; standing at a light while GPS drifts is far below it. */
        const val MOVING_SPEED_MPS = 1.0

        fun forRoute(startedAt: Long, route: List<LocationPoint>) =
            ActiveClock(startedAt, RouteDistance.marks(route))
    }
}
