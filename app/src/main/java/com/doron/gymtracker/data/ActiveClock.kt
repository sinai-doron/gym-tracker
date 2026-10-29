package com.doron.gymtracker.data

/**
 * Moving time with auto-pause: a run pauses once it hasn't moved for [pauseAfterMs] and
 * resumes with the next move. "Moves" are the fixes that counted towards the distance, so
 * GPS jitter while standing still doesn't resume it. The time before a pause kicks in
 * still counts, as a run can't know it stopped until then.
 */
class ActiveClock(
    private val startedAt: Long,
    /** When each move happened, in order. Starting the run counts as one. */
    moves: List<Long>,
    private val pauseAfterMs: Long = PAUSE_AFTER_MS
) {
    private val moves = listOf(startedAt) + moves.filter { it >= startedAt }

    /** Paused stretches between moves, as (pausedAt, resumedAt). */
    val pauses: List<Pair<Long, Long>> = this.moves.zipWithNext()
        .filter { (a, b) -> b - a > pauseAfterMs }
        .map { (a, b) -> (a + pauseAfterMs) to b }

    fun isPaused(now: Long): Boolean = now - (moves.lastOrNull { it <= now } ?: startedAt) > pauseAfterMs

    /** Moving time from the start until [at]. */
    fun activeMs(at: Long): Long {
        var paused = pauses.sumOf { (from, to) -> (minOf(to, at) - from).coerceAtLeast(0) }
        val last = moves.last()
        if (at > last + pauseAfterMs) paused += at - (last + pauseAfterMs)
        return (at - startedAt - paused).coerceAtLeast(0)
    }

    companion object {
        const val PAUSE_AFTER_MS = 10_000L

        fun forRoute(startedAt: Long, route: List<LocationPoint>) =
            ActiveClock(startedAt, RouteDistance.marks(route).map { it.timestamp })
    }
}
