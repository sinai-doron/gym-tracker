package com.doron.gymtracker.data

import com.doron.gymtracker.data.prefs.DistanceUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class RunAnnouncementsTest {

    private val start = 0L
    // 3 km in 16 minutes, moving the whole time
    private val run = CardioRun(
        sessionId = 1, type = "RUNNING", startedAt = start, distanceMeters = 3_000.0, lapMeters = 1_000,
        moves = (start..16 * 60_000L step 2_000).map { DistanceMark(it, it * 3 / 1000.0) }
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

    @Test
    fun `spells durations out in English`() {
        val en = VoiceLanguage.ENGLISH
        assertEquals("5 minutes and 20 seconds", RunAnnouncements.duration(320, en))
        assertEquals("1 minute", RunAnnouncements.duration(60, en))
        assertEquals("1 hour, 3 minutes and 5 seconds", RunAnnouncements.duration(3_785, en))
        assertEquals("45 seconds", RunAnnouncements.duration(45, en))
        assertEquals("0 seconds", RunAnnouncements.duration(0, en))
    }

    @Test
    fun `announces in English`() {
        val en = VoiceLanguage.ENGLISH
        val lap = Lap(3, 1_000.0, 320, 16 * 60_000L)
        assertEquals(
            "Kilometre 3. Lap time 5 minutes and 20 seconds. Average pace 5 minutes and 20 seconds per kilometre",
            RunAnnouncements.text(RunMilestone.LapDone(lap), run, 16 * 60_000L, en)
        )
        assertEquals("Target reached! 3 kilometres in 16 minutes", RunAnnouncements.text(RunMilestone.TargetReached, run, 16 * 60_000L, en))
        assertEquals("Walk paused", RunAnnouncements.text(RunMilestone.Paused, run.copy(type = "WALKING"), 0, en))
        assertEquals("Resuming", RunAnnouncements.text(RunMilestone.Resumed, run, 0, en))
    }

    @Test
    fun `the voice speaks Hebrew for Hebrew and English for every other language`() {
        assertEquals(VoiceLanguage.HEBREW, VoiceLanguage.of("he"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.of("en"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.of("fr"))
    }

    @Test
    fun `in miles, laps are miles and the pace is per mile`() {
        val miles = run.copy(lapMeters = 1_609)
        val lap = Lap(1, 1_609.0, 515, 8 * 60_000L)
        // 16 minutes over 3 km is 8:35 a mile
        assertEquals(
            "Mile 1. Lap time 8 minutes and 35 seconds. Average pace 8 minutes and 35 seconds per mile",
            RunAnnouncements.text(RunMilestone.LapDone(lap), miles, 16 * 60_000L, VoiceLanguage.ENGLISH, DistanceUnit.MI)
        )
        assertEquals(
            "מייל 1. זמן הקפה 8 דקות ו-35 שניות. קצב ממוצע 8 דקות ו-35 שניות למייל",
            RunAnnouncements.text(RunMilestone.LapDone(lap), miles, 16 * 60_000L, VoiceLanguage.HEBREW, DistanceUnit.MI)
        )
        assertEquals(
            "Target reached! 1.9 miles in 16 minutes",
            RunAnnouncements.text(RunMilestone.TargetReached, run, 16 * 60_000L, VoiceLanguage.ENGLISH, DistanceUnit.MI)
        )
    }

    @Test
    fun `a kilometre lap is numbered as a lap for a runner in miles`() {
        val lap = Lap(2, 1_000.0, 320, 0)
        val text = RunAnnouncements.text(RunMilestone.LapDone(lap), run, 16 * 60_000L, VoiceLanguage.ENGLISH, DistanceUnit.MI)
        assertEquals(true, text.startsWith("Lap 2. "))
    }
}
