package com.doron.gymtracker.ui.screens.home

import com.doron.gymtracker.data.prefs.WeekMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakDaysTest {

    private val today = LocalDate.of(2026, 10, 28)
    private fun daysAgo(vararg n: Long) = n.map { today.minusDays(it) }.toSet()

    @Test
    fun `the calendar week starts on the last Sunday, today if it is Sunday`() {
        assertEquals(LocalDate.of(2026, 10, 25), weekStart(today, WeekMode.CALENDAR))
        assertEquals(LocalDate.of(2026, 10, 25), weekStart(LocalDate.of(2026, 10, 25), WeekMode.CALENDAR))
        assertEquals(LocalDate.of(2026, 10, 25), weekStart(LocalDate.of(2026, 10, 31), WeekMode.CALENDAR))
    }

    @Test
    fun `the rolling week is the last seven days, ending today`() {
        assertEquals(LocalDate.of(2026, 10, 22), weekStart(today, WeekMode.ROLLING))
        assertEquals(LocalDate.of(2026, 10, 26), weekStart(LocalDate.of(2026, 11, 1), WeekMode.ROLLING))
    }

    @Test
    fun `no sessions is no streak`() = assertEquals(0, streakDays(emptySet(), today))

    @Test
    fun `counts consecutive days ending today`() = assertEquals(3, streakDays(daysAgo(0, 1, 2), today))

    @Test
    fun `streak ending yesterday still counts`() = assertEquals(2, streakDays(daysAgo(1, 2), today))

    @Test
    fun `a gap ends the streak`() = assertEquals(1, streakDays(daysAgo(1, 3), today))

    @Test
    fun `last session two days ago is no streak`() = assertEquals(0, streakDays(daysAgo(2, 3), today))

    @Test
    fun `week strip colors gym and cardio days, today flagged`() {
        val zone = java.time.ZoneOffset.UTC
        val sunday = LocalDate.of(2026, 10, 25)
        fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()
        val sessions = listOf(
            com.doron.gymtracker.domain.model.Session(1, "workout_a", at(sunday, 9), at(sunday, 10)),
            com.doron.gymtracker.domain.model.Session(2, "", at(sunday, 16), at(sunday, 17), type = "RUNNING"),
            com.doron.gymtracker.domain.model.Session(3, "", at(sunday.plusDays(1), 7), at(sunday.plusDays(1), 8), type = "WALKING")
        )
        val days = weekDays(sunday, sunday.plusDays(3), sessions, zone)
        assertEquals(7, days.size)
        assertEquals(listOf(true, false, false, false, false, false, false), days.map { it.gym })
        assertEquals(listOf(true, true, false, false, false, false, false), days.map { it.cardio })
        assertEquals(listOf(false, false, false, true, false, false, false), days.map { it.isToday })
    }
}
