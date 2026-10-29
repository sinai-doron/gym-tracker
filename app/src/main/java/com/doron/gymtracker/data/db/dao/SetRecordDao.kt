package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.doron.gymtracker.data.db.entities.SetRecordEntity

data class SetWithSession(
    @Embedded val set: SetRecordEntity,
    val sessionId: Long,
    val sessionStartedAt: Long
)

@Dao
interface SetRecordDao {
    @Insert
    suspend fun insert(record: SetRecordEntity): Long

    @Update
    suspend fun update(record: SetRecordEntity)

    /** Every set ever logged for an exercise in a finished session, oldest first. */
    @Query("""
        SELECT sr.*, s.id AS sessionId, s.startedAt AS sessionStartedAt FROM set_records sr
        JOIN exercise_entries ee ON sr.entryId = ee.id
        JOIN sessions s ON s.id = ee.sessionId
        WHERE ee.exerciseId = :exerciseId AND s.finishedAt IS NOT NULL
        ORDER BY s.startedAt, sr.completedAt
    """)
    suspend fun getHistoryForExercise(exerciseId: String): List<SetWithSession>

    @Query("DELETE FROM set_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM set_records WHERE entryId = :entryId")
    suspend fun getForEntry(entryId: Long): List<SetRecordEntity>

    @Query("""
        SELECT sr.* FROM set_records sr
        JOIN exercise_entries ee ON sr.entryId = ee.id
        WHERE ee.sessionId = :sessionId
    """)
    suspend fun getForSession(sessionId: Long): List<SetRecordEntity>

    @Query("DELETE FROM set_records WHERE entryId IN (SELECT id FROM exercise_entries WHERE sessionId = :sessionId)")
    suspend fun deleteForSession(sessionId: Long)
}
