package com.doron.gymtracker.data

/** Something worth telling the runner about, even with the screen off. */
sealed interface RunMilestone {
    data class LapDone(val lap: Lap) : RunMilestone
    data object TargetReached : RunMilestone
    data object Paused : RunMilestone
    data object Resumed : RunMilestone
}

/**
 * Reports each lap and the target once, and each auto-pause and resume, as they happen. Whatever had already happened
 * when it first sees a run (e.g. a run picked back up after the app was killed) is not
 * reported again.
 */
class RunMilestones {
    private var sessionId: Long? = null
    private var lapsSeen = 0
    private var targetReported = false
    private var paused = false

    fun check(run: CardioRun, now: Long): List<RunMilestone> {
        // Moving time: auto-paused time doesn't count towards a duration target
        val elapsedSecs = run.clock.activeMs(now) / 1000
        val reached = run.target.isReached(run.distanceMeters, elapsedSecs)
        val pausedNow = run.clock.isPaused(now)
        if (run.sessionId != sessionId) {
            sessionId = run.sessionId
            lapsSeen = run.laps.size
            targetReported = reached
            paused = pausedNow
            return emptyList()
        }
        val milestones = mutableListOf<RunMilestone>()
        if (pausedNow != paused) {
            paused = pausedNow
            milestones += if (pausedNow) RunMilestone.Paused else RunMilestone.Resumed
        }
        milestones += run.laps.drop(lapsSeen).map { RunMilestone.LapDone(it) }
        lapsSeen = run.laps.size
        if (reached && !targetReported) {
            targetReported = true
            milestones += RunMilestone.TargetReached
        }
        return milestones
    }
}
