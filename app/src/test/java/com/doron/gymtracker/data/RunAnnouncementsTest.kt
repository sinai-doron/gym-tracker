package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RunAnnouncementsTest {

    private val start = 0L
    // 3 km in 16 minutes, moving the whole time
    private val run = CardioRun(
        sessionId = 1, type = "RUNNING", startedAt = start, distanceMeters = 3_000.0, lapMeters = 1_000,
        moves = (start..16 * 60_000L step 2_000).toList()
    )

    @Test
    fun `spells durations out`() {
        assertEquals("5 דקות ו-20 שניות", RunAnnouncements.duration(320))
        assertEquals("דקה", RunAnnouncements.duration(60))
        assertEquals("שעה, 3 דקות ו-5 שניות", RunAnnouncements.duration(3_785))
        assertEquals("45 שניות", RunAnnouncements.duration(45))
    }

    @Test
    fun `announces a kilometer lap`() {
        val lap = Lap(3, 1_000.0, 320, 16 * 60_000L)
        assertEquals(
            "קילומטר 3. זמן הקפה 5 דקות ו-20 שניות. קצב ממוצע 5 דקות ו-20 שניות לקילומטר",
            RunAnnouncements.text(RunMilestone.LapDone(lap), run, 16 * 60_000L)
        )
    }

    @Test
    fun `other lap lengths are numbered`() {
        val lap = Lap(2, 400.0, 100, 0)
        val text = RunAnnouncements.text(RunMilestone.LapDone(lap), run.copy(lapMeters = 400), 16 * 60_000L)
        assertEquals(true, text.startsWith("הקפה 2. זמן הקפה דקה ו-40 שניות"))
    }

    @Test
    fun `announces the target`() {
        assertEquals("הגעת למטרה! 3 קילומטר ב-16 דקות", RunAnnouncements.text(RunMilestone.TargetReached, run, 16 * 60_000L))
    }

    @Test
    fun `pause and resume`() {
        assertEquals("הריצה הושהתה", RunAnnouncements.text(RunMilestone.Paused, run, 0))
        assertEquals("ההליכה הושהתה", RunAnnouncements.text(RunMilestone.Paused, run.copy(type = "WALKING"), 0))
        assertEquals("ממשיכים", RunAnnouncements.text(RunMilestone.Resumed, run, 0))
    }
}
