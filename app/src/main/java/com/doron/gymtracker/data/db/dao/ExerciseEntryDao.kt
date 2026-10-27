package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity

@Dao
interface ExerciseEntryDao {
    @Insert
    suspend fun insert(entry: ExerciseEntryEntity): Long

    @Query("SELECT * FROM exercise_entries WHERE sessionId = :sessionId")
    suspend fun getForSession(sessionId: Long): List<ExerciseEntryEntity>

    @Query("SELECT * FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId LIMIT 1")
    suspend fun getForSessionAndExercise(sessionId: Long, exerciseId: String): ExerciseEntryEntity?

    @Query("DELETE FROM set_records WHERE entryId IN (SELECT id FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId)")
    suspend fun deleteSetsForExercise(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId")
    suspend fun deleteForSessionAndExercise(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM exercise_entries WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
