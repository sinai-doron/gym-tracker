package com.doron.gymtracker.data

/** Something worth telling the runner about, even with the screen off. */
sealed interface RunMilestone {
    data class LapDone(val lap: Lap) : RunMilestone
    data object TargetReached : RunMilestone
}

/**
 * Reports each lap and the target once, as they happen. Whatever had already happened
 * when it first sees a run (e.g. a run picked back up after the app was killed) is not
 * reported again.
 */
class RunMilestones {
    private var sessionId: Long? = null
    private var lapsSeen = 0
    private var targetReported = false

    fun check(run: CardioRun, now: Long): List<RunMilestone> {
        val elapsedSecs = (now - run.startedAt) / 1000
        val reached = run.target.isReached(run.distanceMeters, elapsedSecs)
        if (run.sessionId != sessionId) {
            sessionId = run.sessionId
            lapsSeen = run.laps.size
            targetReported = reached
            return emptyList()
        }
        val milestones = run.laps.drop(lapsSeen).map { RunMilestone.LapDone(it) }.toMutableList<RunMilestone>()
        lapsSeen = run.laps.size
        if (reached && !targetReported) {
            targetReported = true
            milestones += RunMilestone.TargetReached
        }
        return milestones
    }
}
