package com.doron.gymtracker.data.repository

import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.Workout

interface WorkoutRepository {
    suspend fun ensureSeeded()
    suspend fun getAll(): List<Workout>
    suspend fun getById(id: String): Workout?
    suspend fun updateTargetSets(exerciseId: String, sets: Int)
    /** Saves name, sets, reps/distance, rest and bodyweight; the exercise type can't change. */
    suspend fun updateExercise(exercise: ExerciseDefinition)
    suspend fun addExercise(workoutId: String, exercise: ExerciseDefinition)
    suspend fun removeExercise(exerciseId: String)
    /** Moves an exercise one place up (-1) or down (+1) within its workout. */
    suspend fun moveExercise(workoutId: String, exerciseId: String, direction: Int)
}
