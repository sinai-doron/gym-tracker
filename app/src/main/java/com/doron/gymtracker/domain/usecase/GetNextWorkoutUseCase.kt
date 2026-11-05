package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Workout
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** The workout to do next, and the day it falls on when the programme follows the week. */
data class NextWorkout(
    val workout: Workout,
    /** Null when the programme rotates in order: the next workout is whenever the user trains next. */
    val day: LocalDate?
)

/**
 * The workout to do next in the active programme; null while it has none. Read from history, so a
 * deleted session moves it back.
 *
 * - **In order** (no workout has a day): the workout after the last one finished, the first when
 *   none has been done.
 * - **By weekday** (any workout has a day): today's workout, or the next day's that has one. A
 *   workout already done today is passed over; a missed day isn't made up. Workouts without a day
 *   are never suggested: the user starts them from the list.
 */
class GetNextWorkoutUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(): Workout? = next()?.workout

    suspend fun next(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): NextWorkout? {
        val workouts = repo.getAll()
        if (workouts.isEmpty()) return null
        val ids = workouts.map { it.id }
        val done = sessionRepo.getCompletedSince(0).filter { it.type == "GYM" && it.workoutId in ids }

        val scheduled = workouts.filter { it.weekday != null }
        if (scheduled.isEmpty()) {
            val last = done.firstOrNull() ?: return NextWorkout(workouts.first(), null)
            return NextWorkout(workouts[(ids.indexOf(last.workoutId) + 1) % workouts.size], null)
        }

        val doneToday = done.filter { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() == today }
            .map { it.workoutId }.toSet()
        // Up to a week ahead: today's only workout, once done, comes round again next week
        for (offset in 0L..7L) {
            val day = today.plusDays(offset)
            scheduled.firstOrNull { it.weekday == day.dayOfWeek && (offset > 0 || it.id !in doneToday) }
                ?.let { return NextWorkout(it, day) }
        }
        return null
    }
}
