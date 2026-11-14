package com.doron.gymtracker.data

import com.doron.gymtracker.data.prefs.DistanceUnit
import kotlin.math.roundToInt

/** The languages the run voice speaks: Hebrew, and English for every other app language. */
enum class VoiceLanguage(val tag: String) {
    HEBREW("he-IL"), ENGLISH("en");

    companion object {
        /** For an app language as [com.doron.gymtracker.AppLocale.language] gives it ("he", "en", ...). */
        fun of(language: String): VoiceLanguage = if (language == "he") HEBREW else ENGLISH
    }
}

/**
 * What the voice says for each milestone, in Hebrew or English, with distances and pace in
 * kilometres or miles. Times are spelled out as minutes and seconds: "5:20" would be read as a
 * time of day.
 */
object RunAnnouncements {

    fun text(
        milestone: RunMilestone,
        run: CardioRun,
        now: Long,
        language: VoiceLanguage = VoiceLanguage.HEBREW,
        unit: DistanceUnit = DistanceUnit.KM
    ): String = when (language) {
        VoiceLanguage.HEBREW -> Hebrew.text(milestone, run, now, unit)
        VoiceLanguage.ENGLISH -> English.text(milestone, run, now, unit)
    }

    /** "5 דַּקּוֹת וְ-20 שְׁנִיּוֹת", "שָׁעָה וְ-3 דַּקּוֹת", "45 שְׁנִיּוֹת"; "5 minutes and 20 seconds". */
    fun duration(totalSecs: Long, language: VoiceLanguage = VoiceLanguage.HEBREW): String = when (language) {
        VoiceLanguage.HEBREW -> Hebrew.duration(totalSecs)
        VoiceLanguage.ENGLISH -> English.duration(totalSecs)
    }

    private fun movingSecs(run: CardioRun, now: Long) = run.clock.activeMs(now) / 1000

    /** Seconds per kilometre or per mile so far. */
    private fun pace(run: CardioRun, now: Long, unit: DistanceUnit): Long? =
        if (run.distanceMeters > 0) (movingSecs(run, now) * unit.meters / run.distanceMeters).roundToInt().toLong() else null

    /** Kilometres or miles to one decimal, without a trailing ".0". */
    private fun distanceNumber(meters: Double, unit: DistanceUnit): String {
        val rounded = (unit.of(meters) * 10).roundToInt() / 10.0
        return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}" else "$rounded"
    }

    /**
     * Pointed (with niqqud) so the voice reads each word as meant: "הקפה" alone could be read as
     * "the coffee". Nothing is addressed to the runner, so no word needs a gender.
     */
    private object Hebrew {
        fun text(milestone: RunMilestone, run: CardioRun, now: Long, unit: DistanceUnit): String {
            val walk = run.type == "WALKING"
            return when (milestone) {
                RunMilestone.Paused -> if (walk) "הַהֲלִיכָה הוּשְׁהֲתָה" else "הָרִיצָה הוּשְׁהֲתָה"
                RunMilestone.Resumed -> "מַמְשִׁיכִים"
                is RunMilestone.LapDone -> lap(milestone.lap, run, now, unit)
                RunMilestone.TargetReached -> target(run, now, unit)
                RunMilestone.OffRoute -> "מִחוּץ לַמַּסְלוּל"
                RunMilestone.BackOnRoute -> "חֲזָרָה לַמַּסְלוּל"
            }
        }

        private fun word(unit: DistanceUnit) = if (unit == DistanceUnit.KM) "קִילוֹמֶטֶר" else "מַיְל"

        private fun lap(lap: Lap, run: CardioRun, now: Long, unit: DistanceUnit): String {
            val name = unit.wholeUnits(run.lapMeters)?.let { "${word(unit)} ${lap.number * it}" } ?: "הַקָּפָה ${lap.number}"
            val parts = mutableListOf(name, "זְמַן הַקָּפָה ${duration(lap.durationSecs)}")
            pace(run, now, unit)?.let { parts += "קֶצֶב מְמוּצָּע ${duration(it)} לְ${word(unit)}" }
            return parts.joinToString(". ")
        }

        private fun target(run: CardioRun, now: Long, unit: DistanceUnit): String {
            val time = duration(movingSecs(run, now))
            // "בְּ-16 דַּקּוֹת" before a number, "בְּשָׁעָה" before a word, "בִּשְׁעָתַיִים" before a shva
            val inTime = when {
                time.first().isDigit() -> "בְּ-$time"
                time.startsWith("שְׁ") -> "בִּ$time"
                else -> "בְּ$time"
            }
            val done = if (run.routeLengthM > 0) "הַמַּסְלוּל הוּשְׁלַם!" else "הַמַּטָּרָה הוּשְׂגָה!"
            return "$done ${distanceNumber(run.distanceMeters, unit)} ${word(unit)} $inTime"
        }

        fun duration(totalSecs: Long): String {
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            val parts = listOfNotNull(
                when (h) { 0L -> null; 1L -> "שָׁעָה"; 2L -> "שְׁעָתַיִים"; else -> "$h שָׁעוֹת" },
                when (m) { 0L -> null; 1L -> "דַּקָּה"; else -> "$m דַּקּוֹת" },
                when (s) { 0L -> null; 1L -> "שְׁנִיָּה"; else -> "$s שְׁנִיּוֹת" }
            )
            if (parts.isEmpty()) return "0 שְׁנִיּוֹת"
            if (parts.size == 1) return parts[0]
            // "וְ-20" before a number, "וּשְׁנִיָּה" before a shva, "וְדַקָּה" otherwise
            val last = parts.last()
            val and = when {
                last.first().isDigit() -> "וְ-$last"
                last.startsWith("שְׁ") -> "וּ$last"
                else -> "וְ$last"
            }
            return parts.dropLast(1).joinToString(", ") + " " + and
        }
    }

    private object English {
        fun text(milestone: RunMilestone, run: CardioRun, now: Long, unit: DistanceUnit): String {
            val walk = run.type == "WALKING"
            return when (milestone) {
                RunMilestone.Paused -> if (walk) "Walk paused" else "Run paused"
                RunMilestone.Resumed -> "Resuming"
                is RunMilestone.LapDone -> lap(milestone.lap, run, now, unit)
                RunMilestone.TargetReached -> target(run, now, unit)
                RunMilestone.OffRoute -> "You're off the route"
                RunMilestone.BackOnRoute -> "Back on the route"
            }
        }

        private fun word(unit: DistanceUnit) = if (unit == DistanceUnit.KM) "kilometre" else "mile"

        private fun lap(lap: Lap, run: CardioRun, now: Long, unit: DistanceUnit): String {
            val name = unit.wholeUnits(run.lapMeters)?.let { "${word(unit).replaceFirstChar { c -> c.uppercase() }} ${lap.number * it}" }
                ?: "Lap ${lap.number}"
            val parts = mutableListOf(name, "Lap time ${duration(lap.durationSecs)}")
            pace(run, now, unit)?.let { parts += "Average pace ${duration(it)} per ${word(unit)}" }
            return parts.joinToString(". ")
        }

        private fun target(run: CardioRun, now: Long, unit: DistanceUnit): String {
            val done = if (run.routeLengthM > 0) "Route complete!" else "Target reached!"
            val distance = distanceNumber(run.distanceMeters, unit)
            return "$done $distance ${word(unit)}${if (distance == "1") "" else "s"} in ${duration(movingSecs(run, now))}"
        }

        fun duration(totalSecs: Long): String {
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            val parts = listOfNotNull(
                h.takeIf { it > 0 }?.let { "$it ${if (it == 1L) "hour" else "hours"}" },
                m.takeIf { it > 0 }?.let { "$it ${if (it == 1L) "minute" else "minutes"}" },
                s.takeIf { it > 0 }?.let { "$it ${if (it == 1L) "second" else "seconds"}" }
            )
            if (parts.isEmpty()) return "0 seconds"
            return if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " and " + parts.last()
        }
    }
}
