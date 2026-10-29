package com.doron.gymtracker.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StreakDaysTest {

    private val today = LocalDate.of(2026, 10, 28)
    private fun daysAgo(vararg n: Long) = n.map { today.minusDays(it) }.toSet()

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
    fun `week strip marks gym letters over cardio, today flagged`() {
        val zone = java.time.ZoneOffset.UTC
        val sunday = LocalDate.of(2026, 10, 25)
        fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()
        val sessions = listOf(
            com.doron.gymtracker.domain.model.Session(1, "workout_a", at(sunday, 9), at(sunday, 10)),
            com.doron.gymtracker.domain.model.Session(2, "", at(sunday, 16), at(sunday, 17), type = "RUNNING"),
            com.doron.gymtracker.domain.model.Session(3, "", at(sunday.plusDays(1), 7), at(sunday.plusDays(1), 8), type = "WALKING")
        )
        val days = weekDays(sunday, sunday.plusDays(3), sessions, mapOf("workout_a" to "A"), zone)
        assertEquals(7, days.size)
        assertEquals(listOf("A", "🚶", null, null, null, null, null), days.map { it.mark })
        assertEquals(listOf(false, false, false, true, false, false, false), days.map { it.isToday })
    }
}
