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

    /** A programme's workouts, in the order they rotate; hidden ones left out. */
    @Query("SELECT * FROM workouts WHERE programId = :programId AND hiddenAt IS NULL ORDER BY sortOrder, rowid")
    suspend fun getForProgram(programId: String): List<WorkoutEntity>

    /** One workout, hidden or not, in any programme. */
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutEntity?

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET label = :code, name = :name, weekday = :weekday WHERE id = :id")
    suspend fun editWorkout(id: String, code: String, name: String?, weekday: Int?)

    @Query("UPDATE workouts SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setWorkoutSortOrder(id: String, sortOrder: Int)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM workouts WHERE programId = :programId")
    suspend fun maxWorkoutSortOrder(programId: String): Int

    @Query("UPDATE workouts SET hiddenAt = :at WHERE id = :id")
    suspend fun hideWorkout(id: String, at: Long)

    @Query("SELECT count(*) FROM sessions WHERE workoutId = :id")
    suspend fun sessionCount(id: String): Int

    @Query("DELETE FROM exercise_definitions WHERE workoutId = :id")
    suspend fun deleteExercisesOf(id: String)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutRow(id: String)

    /** A workout no session was done in: its exercises go with it. */
    @Transaction
    suspend fun deleteWorkout(id: String) {
        deleteExercisesOf(id)
        deleteWorkoutRow(id)
    }

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
