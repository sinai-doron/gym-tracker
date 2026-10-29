package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import org.junit.Assert.assertEquals
import org.junit.Test

class RunMilestonesTest {

    private val start = 1_000_000L
    private fun lap(n: Int) = Lap(n, 1_000.0, 300, start + n * 300_000L)
    private fun run(distance: Double, laps: Int, target: RunTarget = RunTarget()) = CardioRun(
        sessionId = 1, type = "RUNNING", startedAt = start,
        distanceMeters = distance, target = target, lapMeters = 1_000,
        laps = (1..laps).map(::lap)
    )

    @Test
    fun `reports each new lap once`() {
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0), start)
        assertEquals(listOf(RunMilestone.LapDone(lap(1))), milestones.check(run(1_000.0, 1), start + 300_000))
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(1_200.0, 1), start + 360_000))
    }

    @Test
    fun `reports a distance target once`() {
        val fiveK = RunTarget(RunTargetType.DISTANCE, distanceKm = 5f)
        val milestones = RunMilestones()
        milestones.check(run(4_900.0, 4, fiveK), start)
        assertEquals(
            listOf(RunMilestone.LapDone(lap(5)), RunMilestone.TargetReached),
            milestones.check(run(5_000.0, 5, fiveK), start + 1_500_000)
        )
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(5_100.0, 5, fiveK), start + 1_530_000))
    }

    @Test
    fun `reports a duration target by the clock`() {
        val halfHour = RunTarget(RunTargetType.DURATION, durationMin = 30)
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0, halfHour), start)
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(0.0, 0, halfHour), start + 29 * 60_000))
        assertEquals(listOf(RunMilestone.TargetReached), milestones.check(run(0.0, 0, halfHour), start + 30 * 60_000))
    }

    @Test
    fun `a run picked back up doesn't repeat what already happened`() {
        val fiveK = RunTarget(RunTargetType.DISTANCE, distanceKm = 5f)
        // First seen with 6 laps done and the target passed
        val milestones = RunMilestones()
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(6_000.0, 6, fiveK), start + 1_800_000))
        assertEquals(listOf(RunMilestone.LapDone(lap(7))), milestones.check(run(7_000.0, 7, fiveK), start + 2_100_000))
    }
}
