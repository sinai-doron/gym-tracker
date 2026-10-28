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
}
