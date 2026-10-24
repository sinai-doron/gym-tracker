package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val dao: WorkoutDao
) : WorkoutRepository {

    override suspend fun ensureSeeded() {
        if (dao.getAll().isNotEmpty()) return
        val workoutEntities = TrainingPlanData.all.map { WorkoutEntity(it.id, it.label.name) }
        dao.insertAll(workoutEntities)
        val exerciseEntities = TrainingPlanData.all.flatMap { workout ->
            workout.exercises.map { it.toEntity(workout.id) }
        }
        dao.insertAllExercises(exerciseEntities)
    }

    override suspend fun getAll(): List<Workout> {
        val workoutEntities = dao.getAll()
        return workoutEntities.map { entity ->
            val exercises = dao.getExercisesForWorkout(entity.id).map { it.toDomain() }
            Workout(entity.id, WorkoutLabel.valueOf(entity.label), exercises)
        }
    }

    override suspend fun getById(id: String): Workout? =
        getAll().firstOrNull { it.id == id }

    private fun ExerciseDefinition.toEntity(workoutId: String) = when (this) {
        is RepExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "REP", targetSets, restDurationSecs,
            targetReps = targetReps, targetDistanceM = null,
            isOptional = isOptional, isPerSide = isPerSide
        )
        is TimedExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "TIMED", targetSets, restDurationSecs,
            targetReps = null, targetDistanceM = null,
            isOptional = isOptional, isPerSide = isPerSide
        )
        is DistanceExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "DISTANCE", targetSets, restDurationSecs,
            targetReps = null, targetDistanceM = targetDistanceM,
            isOptional = isOptional, isPerSide = isPerSide
        )
    }

    private fun ExerciseDefinitionEntity.toDomain(): ExerciseDefinition = when (type) {
        "REP" -> RepExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetReps!!,
            isOptional = isOptional, isPerSide = isPerSide
        )
        "DISTANCE" -> DistanceExerciseDefinition(
            id, name, targetSets, restDurationSecs, targetDistanceM!!,
            isOptional = isOptional, isPerSide = isPerSide
        )
        else -> TimedExerciseDefinition(
            id, name, targetSets, restDurationSecs,
            isOptional = isOptional, isPerSide = isPerSide
        )
    }
}
