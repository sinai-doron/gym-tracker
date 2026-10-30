package com.doron.gymtracker.data

import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import org.junit.Assert.assertEquals
import org.junit.Test

class RunMilestonesTest {

    private val start = 1_000_000L
    private fun lap(n: Int) = Lap(n, 1_000.0, 300, start + n * 300_000L)
    private fun run(distance: Double, laps: Int, target: RunTarget = RunTarget(), movingUntil: Long = start) = CardioRun(
        sessionId = 1, type = "RUNNING", startedAt = start,
        distanceMeters = distance, target = target, lapMeters = 1_000,
        laps = (1..laps).map(::lap),
        // A fix that moved the run every 2 s, so auto-pause doesn't stop the clock
        moves = (start..movingUntil step 2_000).map { DistanceMark(it, (it - start) * 3 / 1000.0) }
    )

    @Test
    fun `reports each new lap once`() {
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0), start)
        assertEquals(listOf(RunMilestone.LapDone(lap(1))), milestones.check(run(1_000.0, 1, movingUntil = start + 300_000), start + 300_000))
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(1_200.0, 1, movingUntil = start + 360_000), start + 360_000))
    }

    @Test
    fun `reports a distance target once`() {
        val fiveK = RunTarget(RunTargetType.DISTANCE, distanceKm = 5f)
        val milestones = RunMilestones()
        milestones.check(run(4_900.0, 4, fiveK), start)
        assertEquals(
            listOf(RunMilestone.LapDone(lap(5)), RunMilestone.TargetReached),
            milestones.check(run(5_000.0, 5, fiveK, movingUntil = start + 1_500_000), start + 1_500_000)
        )
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(5_100.0, 5, fiveK, movingUntil = start + 1_530_000), start + 1_530_000))
    }

    @Test
    fun `reports a duration target by the clock`() {
        val halfHour = RunTarget(RunTargetType.DURATION, durationMin = 30)
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0, halfHour), start)
        val at29 = start + 29 * 60_000
        val at30 = start + 30 * 60_000
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(0.0, 0, halfHour, movingUntil = at29), at29))
        assertEquals(listOf(RunMilestone.TargetReached), milestones.check(run(0.0, 0, halfHour, movingUntil = at30), at30))
    }

    @Test
    fun `auto-paused time doesn't count towards a duration target`() {
        val halfHour = RunTarget(RunTargetType.DURATION, durationMin = 30)
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0, halfHour), start)
        // Moved for 20 minutes, then stood still: 30 minutes on the clock isn't 30 minutes run
        val at30 = start + 30 * 60_000
        assertEquals(listOf(RunMilestone.Paused), milestones.check(run(0.0, 0, halfHour, movingUntil = start + 20 * 60_000), at30))
    }

    @Test
    fun `a run picked back up doesn't repeat what already happened`() {
        val fiveK = RunTarget(RunTargetType.DISTANCE, distanceKm = 5f)
        // First seen with 6 laps done and the target passed
        val milestones = RunMilestones()
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(6_000.0, 6, fiveK), start + 1_800_000))
        assertEquals(listOf(RunMilestone.LapDone(lap(7))), milestones.check(run(7_000.0, 7, fiveK), start + 2_100_000))
    }

    @Test
    fun `reports auto-pause and resume once each`() {
        val milestones = RunMilestones()
        milestones.check(run(0.0, 0, movingUntil = start + 60_000), start + 60_000)
        // Stopped at 60 s: paused once 10 s have gone by, not again after
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(0.0, 0, movingUntil = start + 60_000), start + 65_000))
        assertEquals(listOf(RunMilestone.Paused), milestones.check(run(0.0, 0, movingUntil = start + 60_000), start + 75_000))
        assertEquals(emptyList<RunMilestone>(), milestones.check(run(0.0, 0, movingUntil = start + 60_000), start + 90_000))
        // Moving again from 100 s
        val moving = run(0.0, 0, movingUntil = start + 60_000).let { it.copy(moves = it.moves + DistanceMark(start + 100_000, it.moves.last().meters + 3)) }
        assertEquals(listOf(RunMilestone.Resumed), milestones.check(moving, start + 100_000))
    }

    @Test
    fun `a route is done near its end, and leaving it is reported after a while`() {
        val route = RunTarget(RunTargetType.ROUTE, routeId = 1)
        val base = run(0.0, 0, route, movingUntil = start + 120_000).copy(routeLengthM = 5_000.0)
        val milestones = RunMilestones()
        milestones.check(base, start)
        // Off the route for 5 s: not yet; for 12 s: reported once
        val off = base.copy(offRouteSince = start + 60_000)
        assertEquals(emptyList<RunMilestone>(), milestones.check(off, start + 65_000))
        assertEquals(listOf(RunMilestone.OffRoute), milestones.check(off, start + 72_000))
        assertEquals(emptyList<RunMilestone>(), milestones.check(off, start + 80_000))
        // Back on, then finishing within the last 30 m
        val back = base.copy(routeAlongM = 4_980.0, moves = (start..start + 1_500_000 step 2_000).map { DistanceMark(it, (it - start) * 3 / 1000.0) })
        assertEquals(listOf(RunMilestone.BackOnRoute, RunMilestone.TargetReached), milestones.check(back, start + 1_500_000))
    }
}
