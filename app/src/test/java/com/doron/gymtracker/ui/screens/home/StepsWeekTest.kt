package com.doron.gymtracker.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StepsWeekTest {

    // Sunday 25 Oct 2026; "today" is Wednesday the 28th
    private val sunday = LocalDate.of(2026, 10, 25)
    private val wednesday = sunday.plusDays(3)

    private fun week(
        daily: Map<LocalDate, Long>,
        workout: Map<LocalDate, Long> = emptyMap(),
        todaySteps: Long = 0
    ) = StepsWeek.build(sunday, wednesday, daily, workout, todaySteps, goal = 10_000)

    @Test
    fun `days after today are empty and don't count`() {
        val w = week((0L..6L).associate { sunday.plusDays(it) to 12_000L })
        assertEquals(7, w.days.size)
        assertTrue(w.days.drop(4).all { it.isFuture && it.steps == 0L })
        assertEquals(48_000, w.total)
        assertEquals(12_000, w.average)
        assertEquals(4, w.daysAtGoal)
    }

    @Test
    fun `compares with the same days last week`() {
        val lastWeek = (0L..6L).associate { sunday.minusDays(7).plusDays(it) to 5_000L }
        val thisWeek = (0L..3L).associate { sunday.plusDays(it) to 6_000L }
        // 24,000 against last Sunday to Wednesday's 20,000, not last week's whole 35,000
        assertEquals(20, week(lastWeek + thisWeek).changePercent)
    }

    @Test
    fun `no change without last week`() {
        assertNull(week(mapOf(sunday to 8_000L)).changePercent)
    }

    @Test
    fun `workout steps are part of the day and never more than it`() {
        val w = week(mapOf(sunday to 9_000L), workout = mapOf(sunday to 6_000L, sunday.plusDays(1) to 4_000L))
        assertEquals(9_000, w.days[0].steps)
        assertEquals(6_000, w.days[0].workoutSteps)
        // Health Connect had nothing on Monday, but the walk's steps still count
        assertEquals(4_000, w.days[1].steps)
        assertEquals(4_000, w.days[1].workoutSteps)
    }

    @Test
    fun `today takes the phone's counter when it's ahead`() {
        val w = week(mapOf(wednesday to 3_000L), todaySteps = 3_500)
        assertEquals(3_500, w.days[3].steps)
        assertTrue(w.days[3].isToday)
    }
}
