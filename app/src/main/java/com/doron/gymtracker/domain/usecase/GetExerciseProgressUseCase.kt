package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseProgress
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressMetric
import com.doron.gymtracker.domain.model.ProgressPoint
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.TimedSetRecord
import javax.inject.Inject

class GetExerciseProgressUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository
) {
    suspend operator fun invoke(exerciseId: String): ExerciseProgress? {
        val exercise = findExercise(exerciseId) ?: return null
        val metric = when (exercise) {
            is RepExerciseDefinition -> if (exercise.isBodyweight) ProgressMetric.REPS else ProgressMetric.WEIGHT
            is TimedExerciseDefinition -> ProgressMetric.DURATION
            is DistanceExerciseDefinition -> ProgressMetric.DISTANCE
        }
        val history = sessionRepo.getExerciseHistory(exerciseId)
        val points = history.mapNotNull { session ->
            session.bestValue(metric)?.let { ProgressPoint(session.sessionId, session.startedAt, it) }
        }
        val bestValue = points.maxOfOrNull { it.value }
        return ExerciseProgress(
            exercise = exercise,
            metric = metric,
            points = points,
            best = points.lastOrNull { it.value == bestValue },
            sessions = history.reversed()
        )
    }

    // Week-B variants are only reachable through their week-A exercise
    private suspend fun findExercise(id: String): ExerciseDefinition? =
        workoutRepo.getAll().flatMap { w -> w.exercises.flatMap { listOfNotNull(it, it.variantB) } }
            .firstOrNull { it.id == id }

    private fun ExerciseSessionSets.bestValue(metric: ProgressMetric): Float? = when (metric) {
        ProgressMetric.WEIGHT -> sets.filterIsInstance<RepSetRecord>().maxOfOrNull { it.weightKg }
        ProgressMetric.REPS -> sets.filterIsInstance<RepSetRecord>().maxOfOrNull { it.reps.toFloat() }
        ProgressMetric.DURATION -> sets.filterIsInstance<TimedSetRecord>().maxOfOrNull { it.durationSecs.toFloat() }
        ProgressMetric.DISTANCE -> sets.filterIsInstance<DistanceSetRecord>().maxOfOrNull { it.distanceM.toFloat() }
    }
}
