package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Workout
import javax.inject.Inject

/**
 * The workout after the last one finished, in the active programme's order; the first when none
 * of its workouts has been done; null while the programme has none. Read from history, so a
 * deleted session moves it back.
 */
class GetNextWorkoutUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(): Workout? {
        val workouts = repo.getAll()
        if (workouts.isEmpty()) return null
        val ids = workouts.map { it.id }
        val last = sessionRepo.getCompletedSince(0).firstOrNull { it.type == "GYM" && it.workoutId in ids }
            ?: return workouts.first()
        return workouts[(ids.indexOf(last.workoutId) + 1) % workouts.size]
    }
}
