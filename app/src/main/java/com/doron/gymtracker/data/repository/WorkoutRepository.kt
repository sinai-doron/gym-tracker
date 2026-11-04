package com.doron.gymtracker.data.repository

import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.Workout

interface WorkoutRepository {
    suspend fun ensureSeeded()
    /** The programme being followed: its weeks, deloads and variants; null until the user has one. */
    suspend fun getActiveProgram(): Program?
    /** The active programme's workouts, in the order they rotate; none without a programme. */
    suspend fun getAll(): List<Workout>
    /** Any programme's workouts, in the order they rotate; hidden ones left out. */
    suspend fun getForProgram(programId: String): List<Workout>
    /** A workout in any programme, hidden or not. */
    suspend fun getById(id: String): Workout?
    /**
     * Writes [program] whole, in one transaction, and makes it the active one: [workouts] in
     * rotation order, each with its exercises.
     */
    suspend fun installProgram(program: Program, workouts: List<NewWorkout>)
    /** Adds a workout at the end of the programme's rotation; returns its id. */
    suspend fun addWorkout(programId: String, code: String, name: String?): String
    suspend fun editWorkout(id: String, code: String, name: String?)
    /** Moves a workout one place earlier (-1) or later (+1) in its programme's rotation. */
    suspend fun moveWorkout(programId: String, workoutId: String, direction: Int)
    /** Takes a workout out of the rotation: hidden when it has history, deleted otherwise. */
    suspend fun removeWorkout(id: String)
    suspend fun updateTargetSets(exerciseId: String, sets: Int)
    /** Saves name, sets, reps/distance, rest and bodyweight; the exercise type can't change. */
    suspend fun updateExercise(exercise: ExerciseDefinition)
    /** Adds an exercise at the end of the workout; [libraryExerciseId] links it to the library exercise it is. */
    suspend fun addExercise(workoutId: String, exercise: ExerciseDefinition, libraryExerciseId: String? = null)
    suspend fun removeExercise(exerciseId: String)
    /** Moves an exercise one place up (-1) or down (+1) within its workout. */
    suspend fun moveExercise(workoutId: String, exerciseId: String, direction: Int)
}

/** A workout of a programme written at once (a template): its exercises, each with the library exercise it is. */
data class NewWorkout(
    val code: String,
    val name: String?,
    val exercises: List<Pair<ExerciseDefinition, String?>>
)
