package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.doron.gymtracker.data.db.entities.ExerciseNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseNoteDao {
    @Query("SELECT text FROM exercise_notes WHERE exerciseId = :exerciseId")
    fun observe(exerciseId: String): Flow<String?>

    @Upsert
    suspend fun upsert(note: ExerciseNoteEntity)

    @Query("DELETE FROM exercise_notes WHERE exerciseId = :exerciseId")
    suspend fun delete(exerciseId: String)
}
