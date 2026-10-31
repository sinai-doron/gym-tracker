package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.domain.model.AchievementKind
import com.doron.gymtracker.domain.model.AchievementKind.CLIMB_150
import com.doron.gymtracker.domain.model.AchievementKind.CLIMB_50
import com.doron.gymtracker.domain.model.AchievementKind.EARLY_BIRD
import com.doron.gymtracker.domain.model.AchievementKind.IRON_10T
import com.doron.gymtracker.domain.model.AchievementKind.IRON_50T
import com.doron.gymtracker.domain.model.AchievementKind.MILES_25
import com.doron.gymtracker.domain.model.AchievementKind.NIGHT_OWL
import com.doron.gymtracker.domain.model.AchievementKind.REGULAR_2
import com.doron.gymtracker.domain.model.AchievementKind.REGULAR_4
import com.doron.gymtracker.domain.model.AchievementKind.ROTATION
import com.doron.gymtracker.domain.model.AchievementStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class GetAchievementsUseCaseTest {

    private val zone = ZoneId.of("Asia/Jerusalem")
    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun run(id: Long, start: String, km: Double = 5.0, climb: Double? = null, type: String = "RUNNING") =
        SessionFacts(id, type, type, at(start), km * 1000, climb)

    private fun gym(id: Long, start: String, workout: String = "workout_a") = SessionFacts(id, "GYM", workout, at(start))

    private fun compute(sessions: List<SessionFacts>, sets: List<SetFacts> = emptyList()): Map<AchievementKind, AchievementStatus> =
        GetAchievementsUseCase.compute(sessions, sets, setOf("workout_a", "workout_b", "workout_c"), zone).associateBy { it.kind }

    @Test
    fun `climber goes to the first run or walk to climb the height in one go, progress is the best`() {
        val r = compute(
            listOf(
                run(1, "2026-10-01T07:00", climb = 24.0),
                run(2, "2026-10-10T07:00", climb = 65.0, type = "WALKING"),
                run(3, "2026-10-20T07:00", climb = 120.0),
            )
        )
        assertEquals(2L, r.getValue(CLIMB_50).sessionId)
        assertFalse(r.getValue(CLIMB_150).isEarned)
        assertEquals(120.0, r.getValue(CLIMB_150).progress, 0.0)
        assertEquals(0.8f, r.getValue(CLIMB_150).fraction, 0.001f)
    }

    @Test
    fun `mileage adds up runs and walks, earned by the one that crosses the line`() {
        val r = compute((1L..6L).map { run(it, "2026-10-0${it}T07:00", km = 5.0) })
        assertEquals(5L, r.getValue(MILES_25).sessionId)
        assertEquals(30.0, r.getValue(MILES_25).progress, 0.001)
    }

    @Test
    fun `iron counts tonnes lifted, earned by the set that crosses the line`() {
        val sets = (1..25).map { SetFacts(sessionId = if (it <= 20) 1 else 2, completedAt = it.toLong(), volumeKg = 500.0) }
        val r = compute(listOf(gym(1, "2026-10-01T07:00"), gym(2, "2026-10-03T07:00")), sets)
        assertEquals(1L, r.getValue(IRON_10T).sessionId)
        assertEquals(20L, r.getValue(IRON_10T).earnedAt)
        assertFalse(r.getValue(IRON_50T).isEarned)
        assertEquals(12.5, r.getValue(IRON_50T).progress, 0.001)
    }

    @Test
    fun `regular needs consecutive Sunday-to-Saturday weeks of three, and a gap starts over`() {
        // Weeks of Oct 4, 11 and 25 (2026-10-04 is a Sunday): three each; Oct 18 has only two.
        val sessions = listOf(
            gym(1, "2026-10-04T08:00"), run(2, "2026-10-06T08:00"), gym(3, "2026-10-10T20:00"),
            gym(4, "2026-10-11T08:00"), gym(5, "2026-10-13T08:00"), run(6, "2026-10-15T08:00"),
            gym(7, "2026-10-18T08:00"), gym(8, "2026-10-20T08:00"),
            gym(9, "2026-10-25T08:00"), gym(10, "2026-10-27T08:00"), gym(11, "2026-10-29T08:00"),
        )
        val r = compute(sessions)
        assertEquals("the third session of the second week", 6L, r.getValue(REGULAR_2).sessionId)
        assertFalse(r.getValue(REGULAR_4).isEarned)
        assertEquals(2.0, r.getValue(REGULAR_4).progress, 0.0)
    }

    @Test
    fun `rotation needs A, B and C in the same week`() {
        val split = compute(listOf(gym(1, "2026-10-08T08:00", "workout_a"), gym(2, "2026-10-12T08:00", "workout_b"), gym(3, "2026-10-13T08:00", "workout_c")))
        assertFalse("A was the week before", split.getValue(ROTATION).isEarned)
        val week = compute(listOf(gym(1, "2026-10-25T08:00", "workout_a"), gym(2, "2026-10-27T08:00", "workout_c"), gym(3, "2026-10-29T08:00", "workout_b")))
        assertEquals(3L, week.getValue(ROTATION).sessionId)
    }

    @Test
    fun `night owl and early bird go by the local start hour of runs and walks`() {
        val r = compute(listOf(gym(1, "2026-10-01T22:00"), run(2, "2026-10-02T20:59"), run(3, "2026-10-31T21:36"), run(4, "2026-11-01T05:59", type = "WALKING")))
        assertEquals("a gym session doesn't count", 3L, r.getValue(NIGHT_OWL).sessionId)
        assertEquals(4L, r.getValue(EARLY_BIRD).sessionId)
    }

    @Test
    fun `nothing yet means nothing earned and no progress`() {
        val r = compute(emptyList())
        assertTrue(r.values.none { it.isEarned })
        assertNull(r.getValue(MILES_25).sessionId)
        assertEquals(AchievementKind.entries.size, r.size)
    }
}
