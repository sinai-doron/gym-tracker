package com.doron.gymtracker.data

import kotlin.math.roundToInt

/**
 * What the voice says for each milestone, in Hebrew. Times are spelled out as minutes and
 * seconds: "5:20" would be read as a time of day.
 */
object RunAnnouncements {

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
        val name = if (run.lapMeters % 1000 == 0) "קילומטר ${lap.number * run.lapMeters / 1000}" else "הקפה ${lap.number}"
        val parts = mutableListOf(name, "זמן הקפה ${duration(lap.durationSecs)}")
        val movingSecs = run.clock.activeMs(now) / 1000
        if (run.distanceMeters > 0) {
            parts += "קצב ממוצע ${duration((movingSecs * 1000 / run.distanceMeters).roundToInt().toLong())} לקילומטר"
        }
        return parts.joinToString(". ")
    }

    private fun target(run: CardioRun, now: Long): String {
        val movingSecs = run.clock.activeMs(now) / 1000
        val time = duration(movingSecs)
        // "ב-16 דקות" before a number, "בשעה" before a word
        val inTime = if (time.first().isDigit()) "ב-$time" else "ב$time"
        val done = if (run.routeLengthM > 0) "סיימת את המסלול!" else "הגעת למטרה!"
        return "$done ${km(run.distanceMeters)} $inTime"
    }

    /** "5 דקות ו-20 שניות", "שעה ו-3 דקות", "45 שניות". */
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

    private fun km(meters: Double): String {
        val km = meters / 1000
        val rounded = (km * 10).roundToInt() / 10.0
        return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()} קילומטר" else "$rounded קילומטר"
    }
}
