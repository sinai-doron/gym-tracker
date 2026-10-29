package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ExercisePreview
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutPreview
import javax.inject.Inject

/** What a workout looks like before starting it: today's exercises, last weights, history. */
class GetWorkoutPreviewUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val getLastWeight: GetLastWeightUseCase,
    private val resolveVariant: ResolveExerciseVariantUseCase
) {
    suspend operator fun invoke(workout: Workout, now: Long = System.currentTimeMillis()): WorkoutPreview {
        val pastSessions = sessionRepo.getCompletedSince(0)
            .filter { it.type == "GYM" && it.workoutId == workout.id }
        val recentMinutes = pastSessions.take(TYPICAL_DURATION_SAMPLE)
            .mapNotNull { s -> s.finishedAt?.let { (it - s.startedAt) / 60_000 } }
            .filter { it > 0 }
        return WorkoutPreview(
            workout = workout,
            exercises = workout.exercises.map { resolveVariant(it, now) }.map {
                val bodyweight = it is RepExerciseDefinition && it.isBodyweight
                ExercisePreview(it, if (bodyweight) null else getLastWeight(it.id))
            },
            lastDoneAt = pastSessions.firstOrNull()?.startedAt,
            typicalMinutes = if (recentMinutes.isEmpty()) null else recentMinutes.average().toInt()
        )
    }

    private companion object {
        const val TYPICAL_DURATION_SAMPLE = 5
    }
}
