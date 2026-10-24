package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
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
            id, workoutId, name, "REP", targetSets, restDurationSecs, targetReps
        )
        is TimedExerciseDefinition -> ExerciseDefinitionEntity(
            id, workoutId, name, "TIMED", targetSets, restDurationSecs, null
        )
    }

    private fun ExerciseDefinitionEntity.toDomain(): ExerciseDefinition = when (type) {
        "REP" -> RepExerciseDefinition(id, name, targetSets, restDurationSecs, targetReps!!)
        else -> TimedExerciseDefinition(id, name, targetSets, restDurationSecs)
    }
}
