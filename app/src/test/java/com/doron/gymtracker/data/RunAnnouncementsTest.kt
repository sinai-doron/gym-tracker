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
        assertEquals("5 דַּקּוֹת וְ-20 שְׁנִיּוֹת", RunAnnouncements.duration(320))
        assertEquals("דַּקָּה", RunAnnouncements.duration(60))
        assertEquals("שְׁעָתַיִים, דַּקָּה וּשְׁנִיָּה", RunAnnouncements.duration(7_261))
        assertEquals("שָׁעָה וְ" + "דַּקָּה", RunAnnouncements.duration(3_660))
        assertEquals("שָׁעָה, 3 דַּקּוֹת וְ-5 שְׁנִיּוֹת", RunAnnouncements.duration(3_785))
        assertEquals("45 שְׁנִיּוֹת", RunAnnouncements.duration(45))
    }

    @Test
    fun `announces a kilometer lap`() {
        val lap = Lap(3, 1_000.0, 320, 16 * 60_000L)
        assertEquals(
            "קִילוֹמֶטֶר 3. זְמַן הַקָּפָה 5 דַּקּוֹת וְ-20 שְׁנִיּוֹת. קֶצֶב מְמוּצָּע 5 דַּקּוֹת וְ-20 שְׁנִיּוֹת לְקִילוֹמֶטֶר",
            RunAnnouncements.text(RunMilestone.LapDone(lap), run, 16 * 60_000L)
        )
    }

    @Test
    fun `other lap lengths are numbered`() {
        val lap = Lap(2, 400.0, 100, 0)
        val text = RunAnnouncements.text(RunMilestone.LapDone(lap), run.copy(lapMeters = 400), 16 * 60_000L)
        assertEquals(true, text.startsWith("הַקָּפָה 2. זְמַן הַקָּפָה דַּקָּה וְ-40 שְׁנִיּוֹת"))
    }

    @Test
    fun `announces the target`() {
        assertEquals("הַמַּטָּרָה הוּשְׂגָה! 3 קִילוֹמֶטֶר בְּ-16 דַּקּוֹת", RunAnnouncements.text(RunMilestone.TargetReached, run, 16 * 60_000L))
        val slow = run.copy(moves = (start..2 * 3_600_000L step 60_000).map { DistanceMark(it, it * 3 / 1000.0) })
        assertEquals("הַמַּטָּרָה הוּשְׂגָה! 3 קִילוֹמֶטֶר בִּשְׁעָתַיִים", RunAnnouncements.text(RunMilestone.TargetReached, slow, 2 * 3_600_000L))
    }

    @Test
    fun `pause and resume`() {
        assertEquals("הָרִיצָה הוּשְׁהֲתָה", RunAnnouncements.text(RunMilestone.Paused, run, 0))
        assertEquals("הַהֲלִיכָה הוּשְׁהֲתָה", RunAnnouncements.text(RunMilestone.Paused, run.copy(type = "WALKING"), 0))
        assertEquals("מַמְשִׁיכִים", RunAnnouncements.text(RunMilestone.Resumed, run, 0))
    }

    @Test
    fun `leaving and rejoining the route`() {
        assertEquals("מִחוּץ לַמַּסְלוּל", RunAnnouncements.text(RunMilestone.OffRoute, run, 0))
        assertEquals("חֲזָרָה לַמַּסְלוּל", RunAnnouncements.text(RunMilestone.BackOnRoute, run, 0))
        assertEquals("הַמַּסְלוּל הוּשְׁלַם! 3 קִילוֹמֶטֶר בְּ-16 דַּקּוֹת", RunAnnouncements.text(RunMilestone.TargetReached, run.copy(routeLengthM = 3_000.0), 16 * 60_000L))
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
            "מַיְל 1. זְמַן הַקָּפָה 8 דַּקּוֹת וְ-35 שְׁנִיּוֹת. קֶצֶב מְמוּצָּע 8 דַּקּוֹת וְ-35 שְׁנִיּוֹת לְמַיְל",
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
