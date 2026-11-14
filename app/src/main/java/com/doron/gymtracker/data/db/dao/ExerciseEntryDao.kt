package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity

/** A note on an exercise's sets, and when that session began. */
data class EntryNote(val note: String, val startedAt: Long)

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

    /** The session's entry for a plan exercise, made (with no sets) when there isn't one yet. */
    @Transaction
    suspend fun getOrCreate(sessionId: Long, exerciseId: String): ExerciseEntryEntity =
        getForSessionAndExercise(sessionId, exerciseId)
            ?: insert(ExerciseEntryEntity(0, sessionId, exerciseId)).let { getForSessionAndExercise(sessionId, exerciseId)!! }

    @Query("UPDATE exercise_entries SET skipped = :skipped WHERE id = :entryId")
    suspend fun setSkipped(entryId: Long, skipped: Boolean)

    @Query("UPDATE exercise_entries SET note = :note WHERE id = :entryId")
    suspend fun setNote(entryId: Long, note: String?)

    /** The newest note on a library exercise's sets from another session than [sessionId]. */
    @Query(
        """SELECT e.note AS note, s.startedAt AS startedAt FROM exercise_entries e JOIN sessions s ON s.id = e.sessionId
           WHERE e.libraryExerciseId = :libraryExerciseId AND e.sessionId != :sessionId AND trim(coalesce(e.note, '')) != ''
           ORDER BY s.startedAt DESC LIMIT 1"""
    )
    suspend fun lastNote(libraryExerciseId: String, sessionId: Long): EntryNote?

    @Query("DELETE FROM exercise_entries WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
