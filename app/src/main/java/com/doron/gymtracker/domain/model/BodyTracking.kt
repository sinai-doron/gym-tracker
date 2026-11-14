package com.doron.gymtracker.domain.model

import com.doron.gymtracker.data.prefs.WaterReminder
import com.doron.gymtracker.data.prefs.WeighInReminder
import com.doron.gymtracker.data.prefs.WeightReminder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil
import kotlin.math.roundToInt

data class WeighIn(val id: Long, val measuredAt: Long, val kg: Double)

data class WeightGoal(val startKg: Double, val startAt: Long, val targetKg: Double, val paceKgPerWeek: Double)

data class Drink(val id: Long, val drankAt: Long, val ml: Int)

data class WaterGoal(val dailyMl: Int = 2_500, val glassMl: Int = 250, val bottleMl: Int = 500) {
    companion object {
        const val MIN_DAILY = 500
        const val MAX_DAILY = 10_000
        /** Roughly what a day needs per kg of body weight. */
        const val ML_PER_KG = 35

        /** A goal for [kg], rounded to a 100 ml. */
        fun suggestedFor(kg: Double): Int = ((kg * ML_PER_KG / 100).roundToInt() * 100).coerceIn(MIN_DAILY, MAX_DAILY)
    }
}

/** What the weight screens show, worked out from the weigh-ins (newest first) and the goal. */
object WeightStats {
    const val MIN_KG = 20.0
    const val MAX_KG = 400.0
    private const val TREND_DAYS = 7L

    /** The 7-day trend at each weigh-in: the average of the ones in the 7 days up to it. Oldest first. */
    fun trend(entries: List<WeighIn>): List<Pair<Long, Double>> {
        val asc = entries.sortedBy { it.measuredAt }
        val window = TREND_DAYS * 86_400_000L
        return asc.map { e ->
            val inWindow = asc.filter { it.measuredAt in (e.measuredAt - window + 1)..e.measuredAt }
            e.measuredAt to inWindow.map { it.kg }.average()
        }
    }

    /** The average of the weigh-ins of the last 7 days up to [now]; null with none. */
    fun weekAverage(entries: List<WeighIn>, now: Long): Double? =
        entries.filter { it.measuredAt in (now - TREND_DAYS * 86_400_000L + 1)..now }.map { it.kg }.takeIf { it.isNotEmpty() }?.average()

    /** This week's average against the week before's; null without both. */
    fun weekChange(entries: List<WeighIn>, now: Long): Double? {
        val thisWeek = weekAverage(entries, now) ?: return null
        val lastWeek = weekAverage(entries, now - TREND_DAYS * 86_400_000L) ?: return null
        return thisWeek - lastWeek
    }

    /** Share of the way from the goal's start to its target, 0 to 1. */
    fun progress(goal: WeightGoal, currentKg: Double): Float {
        val span = goal.startKg - goal.targetKg
        if (span == 0.0) return 1f
        return ((goal.startKg - currentKg) / span).toFloat().coerceIn(0f, 1f)
    }

    /** Weeks to the target at the goal's pace; 0 when there. */
    fun weeksToGo(currentKg: Double, targetKg: Double, paceKgPerWeek: Double): Int =
        if (paceKgPerWeek <= 0) 0 else ceil(kotlin.math.abs(currentKg - targetKg) / paceKgPerWeek - 1e-9).toInt()
}

/** When reminders ring next. */
object Reminders {

    /** The next weigh-in reminder after [now]; null when off. */
    fun nextWeighIn(r: WeightReminder, now: LocalDateTime): LocalDateTime? {
        val at = LocalTime.of(r.atMinute / 60, r.atMinute % 60)
        return when (r.frequency) {
            WeighInReminder.OFF -> null
            WeighInReminder.DAILY -> now.toLocalDate().atTime(at).let { if (it.isAfter(now)) it else it.plusDays(1) }
            WeighInReminder.WEEKLY -> {
                val day = now.toLocalDate().with(TemporalAdjusters.nextOrSame(r.day)).atTime(at)
                if (day.isAfter(now)) day else day.plusWeeks(1)
            }
        }
    }

    /** The next water reminder after [now]: every few hours between from and until; null when off. */
    fun nextWater(r: WaterReminder, now: LocalDateTime): LocalDateTime? {
        if (!r.on || r.everyHours <= 0 || r.untilMinute <= r.fromMinute) return null
        var day = now.toLocalDate()
        repeat(2) {
            var m = r.fromMinute
            while (m <= r.untilMinute) {
                val t = day.atStartOfDay().plusMinutes(m.toLong())
                if (t.isAfter(now)) return t
                m += r.everyHours * 60
            }
            day = day.plusDays(1)
        }
        return null
    }

    /**
     * Whether the day's drinking is ahead of its pace at [now]: the goal spread evenly over the
     * reminder hours, so by the middle of them half of it.
     */
    fun waterOnTrack(totalMl: Int, goalMl: Int, r: WaterReminder, now: LocalDateTime): Boolean {
        val minute = now.hour * 60 + now.minute
        val share = ((minute - r.fromMinute).toDouble() / (r.untilMinute - r.fromMinute)).coerceIn(0.0, 1.0)
        return totalMl >= goalMl * share
    }

    fun startOfDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun millisUntil(t: LocalDateTime, now: LocalDateTime): Long = ChronoUnit.MILLIS.between(now, t).coerceAtLeast(0)
}
