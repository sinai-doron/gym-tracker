package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(workouts: List<WorkoutEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllExercises(exercises: List<ExerciseDefinitionEntity>)

    @Transaction
    suspend fun seed(workouts: List<WorkoutEntity>, exercises: List<ExerciseDefinitionEntity>) {
        insertAll(workouts)
        insertAllExercises(exercises)
    }

    @Query("SELECT * FROM workouts")
    suspend fun getAll(): List<WorkoutEntity>

    @Query("UPDATE exercise_definitions SET targetSets = :sets WHERE id = :exerciseId")
    suspend fun updateTargetSets(exerciseId: String, sets: Int)

    @Query("SELECT * FROM exercise_definitions WHERE workoutId = :workoutId AND isRemoved = 0 ORDER BY sortOrder, rowid")
    suspend fun getExercisesForWorkout(workoutId: String): List<ExerciseDefinitionEntity>

    @Query("SELECT * FROM exercise_definitions WHERE id = :id")
    suspend fun getExercise(id: String): ExerciseDefinitionEntity?

    @Update
    suspend fun updateExercise(exercise: ExerciseDefinitionEntity)

    @Insert
    suspend fun insertExercise(exercise: ExerciseDefinitionEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM exercise_definitions WHERE workoutId = :workoutId")
    suspend fun maxSortOrder(workoutId: String): Int

    @Query("UPDATE exercise_definitions SET isRemoved = 1 WHERE id = :id")
    suspend fun markRemoved(id: String)

    @Query("UPDATE exercise_definitions SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: String, sortOrder: Int)
}
