package com.doron.gymtracker.ui.screens.log

import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class LogSessionStateTest {

    private val strength = TrainingType("strength", null, "dumbbell", "#6C5CE7", Recorder.EXERCISES, 70, 5.0, true, 1, false)
    private val football = TrainingType("f", "Football", "football", "#00CEC9", Recorder.DURATION, 0, 7.0, false, 20, false)
    private val yesterday = LogSessionState(
        types = listOf(strength, football),
        typeId = "f",
        date = LocalDate.now().minusDays(1),
        start = LocalTime.of(19, 30),
        hours = "1",
        minutes = "30",
        bodyWeightKg = 78f,
    )

    @Test
    fun `a game yesterday can be saved, with its calories estimated`() {
        assertNull(yesterday.problem())
        assertEquals(5400, yesterday.durationS)
        assertEquals(819, yesterday.estimatedCalories)
    }

    @Test
    fun `it can't end in the future, or last nothing`() {
        val tomorrow = yesterday.copy(date = LocalDate.now().plusDays(1))
        assertEquals(LogProblem.IN_THE_FUTURE, tomorrow.problem())
        assertEquals(LogProblem.NO_DURATION, yesterday.copy(hours = "0", minutes = "").problem())
    }

    @Test
    fun `strength needs a workout`() {
        val gym = yesterday.copy(typeId = "strength")
        assertEquals(LogProblem.NO_WORKOUT, gym.problem())
        assertNull(gym.copy(workoutId = "workout_a").problem())
    }

    @Test
    fun `the start is the date and time in the phone's zone`() {
        val zone = ZoneId.of("Asia/Jerusalem")
        val s = yesterday.copy(date = LocalDate.of(2026, 11, 2), start = LocalTime.of(19, 30))
        assertEquals(LocalDate.of(2026, 11, 2).atTime(19, 30).atZone(zone).toInstant().toEpochMilli(), s.startMs(zone))
    }
}
