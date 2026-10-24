package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<ExerciseDefinitionEntity>)

    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("SELECT * FROM exercise_definitions WHERE workoutId = :workoutId")
    suspend fun getExercisesForWorkout(workoutId: String): List<ExerciseDefinitionEntity>
}
