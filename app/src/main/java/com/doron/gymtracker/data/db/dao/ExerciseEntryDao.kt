package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity

@Dao
interface ExerciseEntryDao {
    @Insert
    suspend fun insertRow(entry: ExerciseEntryEntity): Long

    @Query("SELECT libraryExerciseId FROM exercise_definitions WHERE id = :planId")
    suspend fun libraryIdOf(planId: String): String?

    /** Inserts an entry, linked to the library exercise its plan exercise is. */
    @Transaction
    suspend fun insert(entry: ExerciseEntryEntity): Long =
        insertRow(entry.copy(libraryExerciseId = entry.libraryExerciseId ?: libraryIdOf(entry.exerciseId)))

    @Query("SELECT * FROM exercise_entries WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: Long): List<ExerciseEntryEntity>

    @Query("SELECT * FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getForSessionAndExercise(sessionId: Long, exerciseId: String): ExerciseEntryEntity?

    @Query("DELETE FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteForSessionAndExercise(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM exercise_entries WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
