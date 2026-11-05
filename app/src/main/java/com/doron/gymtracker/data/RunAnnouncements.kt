package com.doron.gymtracker.data

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
 * What the voice says for each milestone, in Hebrew or English. Times are spelled out as
 * minutes and seconds: "5:20" would be read as a time of day.
 */
object RunAnnouncements {

    fun text(milestone: RunMilestone, run: CardioRun, now: Long, language: VoiceLanguage = VoiceLanguage.HEBREW): String =
        when (language) {
            VoiceLanguage.HEBREW -> Hebrew.text(milestone, run, now)
            VoiceLanguage.ENGLISH -> English.text(milestone, run, now)
        }

    /** "5 דקות ו-20 שניות", "שעה ו-3 דקות", "45 שניות"; "5 minutes and 20 seconds". */
    fun duration(totalSecs: Long, language: VoiceLanguage = VoiceLanguage.HEBREW): String = when (language) {
        VoiceLanguage.HEBREW -> Hebrew.duration(totalSecs)
        VoiceLanguage.ENGLISH -> English.duration(totalSecs)
    }

    private fun movingSecs(run: CardioRun, now: Long) = run.clock.activeMs(now) / 1000

    private fun paceSecsPerKm(run: CardioRun, now: Long): Long? =
        if (run.distanceMeters > 0) (movingSecs(run, now) * 1000 / run.distanceMeters).roundToInt().toLong() else null

    /** Kilometres to one decimal, without a trailing ".0". */
    private fun kmNumber(meters: Double): String {
        val rounded = (meters / 1000 * 10).roundToInt() / 10.0
        return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}" else "$rounded"
    }

    private fun isWholeKm(run: CardioRun) = run.lapMeters % 1000 == 0

    private object Hebrew {
        fun text(milestone: RunMilestone, run: CardioRun, now: Long): String {
            val walk = run.type == "WALKING"
            return when (milestone) {
                RunMilestone.Paused -> if (walk) "ההליכה הושהתה" else "הריצה הושהתה"
                RunMilestone.Resumed -> "ממשיכים"
                is RunMilestone.LapDone -> lap(milestone.lap, run, now)
                RunMilestone.TargetReached -> target(run, now)
                RunMilestone.OffRoute -> "יצאת מהמסלול"
                RunMilestone.BackOnRoute -> "חזרת למסלול"
            }
        }

        private fun lap(lap: Lap, run: CardioRun, now: Long): String {
            val name = if (isWholeKm(run)) "קילומטר ${lap.number * run.lapMeters / 1000}" else "הקפה ${lap.number}"
            val parts = mutableListOf(name, "זמן הקפה ${duration(lap.durationSecs)}")
            paceSecsPerKm(run, now)?.let { parts += "קצב ממוצע ${duration(it)} לקילומטר" }
            return parts.joinToString(". ")
        }

        private fun target(run: CardioRun, now: Long): String {
            val time = duration(movingSecs(run, now))
            // "ב-16 דקות" before a number, "בשעה" before a word
            val inTime = if (time.first().isDigit()) "ב-$time" else "ב$time"
            val done = if (run.routeLengthM > 0) "סיימת את המסלול!" else "הגעת למטרה!"
            return "$done ${kmNumber(run.distanceMeters)} קילומטר $inTime"
        }

        fun duration(totalSecs: Long): String {
            val h = totalSecs / 3600
            val m = (totalSecs % 3600) / 60
            val s = totalSecs % 60
            val parts = listOfNotNull(
                when (h) { 0L -> null; 1L -> "שעה"; 2L -> "שעתיים"; else -> "$h שעות" },
                when (m) { 0L -> null; 1L -> "דקה"; else -> "$m דקות" },
                when (s) { 0L -> null; 1L -> "שנייה"; else -> "$s שניות" }
            )
            if (parts.isEmpty()) return "0 שניות"
            return if (parts.size == 1) parts[0] else parts.dropLast(1).joinToString(", ") + " ו-" + parts.last()
        }
    }

    private object English {
        fun text(milestone: RunMilestone, run: CardioRun, now: Long): String {
            val walk = run.type == "WALKING"
            return when (milestone) {
                RunMilestone.Paused -> if (walk) "Walk paused" else "Run paused"
                RunMilestone.Resumed -> "Resuming"
                is RunMilestone.LapDone -> lap(milestone.lap, run, now)
                RunMilestone.TargetReached -> target(run, now)
                RunMilestone.OffRoute -> "You're off the route"
                RunMilestone.BackOnRoute -> "Back on the route"
            }
        }

        private fun lap(lap: Lap, run: CardioRun, now: Long): String {
            val name = if (isWholeKm(run)) "Kilometre ${lap.number * run.lapMeters / 1000}" else "Lap ${lap.number}"
            val parts = mutableListOf(name, "Lap time ${duration(lap.durationSecs)}")
            paceSecsPerKm(run, now)?.let { parts += "Average pace ${duration(it)} per kilometre" }
            return parts.joinToString(". ")
        }

        private fun target(run: CardioRun, now: Long): String {
            val done = if (run.routeLengthM > 0) "Route complete!" else "Target reached!"
            val km = kmNumber(run.distanceMeters)
            return "$done $km ${if (km == "1") "kilometre" else "kilometres"} in ${duration(movingSecs(run, now))}"
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
