package com.doron.gymtracker.ui.screens.home

import java.time.LocalDate
import kotlin.math.roundToInt

/** One day of the week's steps; [workoutSteps] of them were taken on runs and walks. */
data class StepsDay(
    val date: LocalDate,
    val steps: Long,
    val workoutSteps: Long,
    val isToday: Boolean,
    val isFuture: Boolean
)

/** Steps for each day of this week, Sunday to Saturday, against the daily [goal]. */
data class StepsWeek(
    val days: List<StepsDay>,
    val goal: Int,
    /** Change from last week over the same days (Sunday to today), in percent; null without last week's steps. */
    val changePercent: Int?
) {
    private val sofar get() = days.filterNot { it.isFuture }
    val total: Long get() = sofar.sumOf { it.steps }
    /** Per day so far, today included. */
    val average: Long get() = if (sofar.isEmpty()) 0 else total / sofar.size
    val daysAtGoal: Int get() = sofar.count { it.steps >= goal }

    companion object {
        /**
         * The week starting [weekStart], from Health Connect's [daily] steps (which may also
         * hold last week) and the [workout] steps recorded on runs and walks. A day never shows
         * fewer steps than its workouts took, in case Health Connect missed them; [todaySteps]
         * from the phone's own counter can be ahead of Health Connect for today.
         */
        fun build(
            weekStart: LocalDate,
            today: LocalDate,
            daily: Map<LocalDate, Long>,
            workout: Map<LocalDate, Long>,
            todaySteps: Long,
            goal: Int
        ): StepsWeek {
            val days = (0L until 7L).map { i ->
                val date = weekStart.plusDays(i)
                val counted = daily[date] ?: 0L
                val steps = maxOf(counted, workout[date] ?: 0L, if (date == today) todaySteps else 0L)
                StepsDay(
                    date = date,
                    steps = if (date.isAfter(today)) 0 else steps,
                    workoutSteps = if (date.isAfter(today)) 0 else minOf(workout[date] ?: 0L, steps),
                    isToday = date == today,
                    isFuture = date.isAfter(today)
                )
            }
            val elapsed = days.count { !it.isFuture }.toLong()
            val lastWeek = (0L until elapsed).sumOf { daily[weekStart.minusDays(7).plusDays(it)] ?: 0L }
            val thisWeek = days.filterNot { it.isFuture }.sumOf { it.steps }
            val change = if (lastWeek > 0) ((thisWeek - lastWeek) * 100.0 / lastWeek).roundToInt() else null
            return StepsWeek(days, goal, change)
        }
    }
}
