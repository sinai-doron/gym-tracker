package com.doron.gymtracker.domain.model

import com.doron.gymtracker.data.prefs.WaterReminder
import com.doron.gymtracker.data.prefs.WeighInReminder
import com.doron.gymtracker.data.prefs.WeightReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

class BodyTrackingTest {

    private val day = 86_400_000L
    private val t0 = 1_700_000_000_000L

    private fun weighIn(daysAgo: Int, kg: Double, now: Long = t0) = WeighIn(daysAgo.toLong(), now - daysAgo * day, kg)

    @Test
    fun `trend averages the weigh-ins of the 7 days up to each`() {
        val entries = listOf(weighIn(0, 80.0), weighIn(1, 82.0), weighIn(8, 90.0))
        val trend = WeightStats.trend(entries)
        assertEquals(3, trend.size)
        assertEquals(90.0, trend[0].second, 1e-9)    // alone in its week
        assertEquals(82.0, trend[1].second, 1e-9)    // the 90 is 7 days back: out of the window
        assertEquals(81.0, trend[2].second, 1e-9)
        assertTrue(trend.zipWithNext().all { (a, b) -> a.first < b.first })
    }

    @Test
    fun `week change compares this week's average with the week before's`() {
        val entries = listOf(weighIn(0, 80.0), weighIn(2, 81.0), weighIn(8, 83.0), weighIn(10, 82.0))
        assertEquals(80.5, WeightStats.weekAverage(entries, t0)!!, 1e-9)
        assertEquals(-2.0, WeightStats.weekChange(entries, t0)!!, 1e-9)
        assertNull(WeightStats.weekChange(listOf(weighIn(0, 80.0)), t0))
    }

    @Test
    fun `progress runs from the start to the target either way`() {
        val losing = WeightGoal(startKg = 86.0, startAt = 0, targetKg = 78.0, paceKgPerWeek = 0.5)
        assertEquals(0.45f, WeightStats.progress(losing, 82.4), 1e-4f)
        assertEquals(0f, WeightStats.progress(losing, 87.0), 0f)
        assertEquals(1f, WeightStats.progress(losing, 77.0), 0f)
        val gaining = WeightGoal(startKg = 60.0, startAt = 0, targetKg = 64.0, paceKgPerWeek = 0.25)
        assertEquals(0.5f, WeightStats.progress(gaining, 62.0), 1e-4f)
    }

    @Test
    fun `weeks to go round up, none when there`() {
        assertEquals(9, WeightStats.weeksToGo(82.4, 78.0, 0.5))
        assertEquals(8, WeightStats.weeksToGo(82.0, 78.0, 0.5))
        assertEquals(0, WeightStats.weeksToGo(78.0, 78.0, 0.5))
    }

    @Test
    fun `suggested water goal is 35 ml a kg to the 100, within bounds`() {
        assertEquals(2_900, WaterGoal.suggestedFor(82.4))
        assertEquals(WaterGoal.MIN_DAILY, WaterGoal.suggestedFor(10.0))
        assertEquals(WaterGoal.MAX_DAILY, WaterGoal.suggestedFor(400.0))
    }

    @Test
    fun `daily weigh-in reminder is today's time, else tomorrow's`() {
        val r = WeightReminder(WeighInReminder.DAILY, atMinute = 7 * 60 + 30)
        assertEquals(LocalDateTime.of(2026, 11, 14, 7, 30), Reminders.nextWeighIn(r, LocalDateTime.of(2026, 11, 14, 6, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 15, 7, 30), Reminders.nextWeighIn(r, LocalDateTime.of(2026, 11, 14, 7, 30)))
        assertNull(Reminders.nextWeighIn(r.copy(frequency = WeighInReminder.OFF), LocalDateTime.of(2026, 11, 14, 6, 0)))
    }

    @Test
    fun `weekly weigh-in reminder is the next chosen day`() {
        // 14 Nov 2026 is a Saturday
        val r = WeightReminder(WeighInReminder.WEEKLY, DayOfWeek.SUNDAY, 8 * 60)
        assertEquals(LocalDateTime.of(2026, 11, 15, 8, 0), Reminders.nextWeighIn(r, LocalDateTime.of(2026, 11, 14, 20, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 22, 8, 0), Reminders.nextWeighIn(r, LocalDateTime.of(2026, 11, 15, 9, 0)))
    }

    @Test
    fun `water reminders ring every few hours between from and until`() {
        val r = WaterReminder(on = true, everyHours = 2, fromMinute = 8 * 60, untilMinute = 22 * 60)
        assertEquals(LocalDateTime.of(2026, 11, 14, 8, 0), Reminders.nextWater(r, LocalDateTime.of(2026, 11, 14, 3, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 14, 14, 0), Reminders.nextWater(r, LocalDateTime.of(2026, 11, 14, 12, 0)))
        assertEquals(LocalDateTime.of(2026, 11, 14, 22, 0), Reminders.nextWater(r, LocalDateTime.of(2026, 11, 14, 21, 59)))
        assertEquals(LocalDateTime.of(2026, 11, 15, 8, 0), Reminders.nextWater(r, LocalDateTime.of(2026, 11, 14, 22, 0)))
        assertNull(Reminders.nextWater(r.copy(on = false), LocalDateTime.of(2026, 11, 14, 12, 0)))
    }

    @Test
    fun `on track means ahead of the goal spread over the reminder hours`() {
        val r = WaterReminder(on = true, fromMinute = 8 * 60, untilMinute = 22 * 60)
        val noon = LocalDateTime.of(2026, 11, 14, 15, 0)    // half way through the hours
        assertTrue(Reminders.waterOnTrack(1_250, 2_500, r, noon))
        assertFalse(Reminders.waterOnTrack(1_000, 2_500, r, noon))
        assertTrue(Reminders.waterOnTrack(0, 2_500, r, LocalDateTime.of(2026, 11, 14, 7, 0)))
    }
}
