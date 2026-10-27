package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.TrackLogEntity

@Dao
interface TrackLogDao {
    @Insert
    suspend fun insert(entity: TrackLogEntity): Long

    @Query("SELECT * FROM track_log WHERE sessionId = :sessionId ORDER BY startedAt ASC")
    suspend fun getForSession(sessionId: Long): List<TrackLogEntity>

    @Query("DELETE FROM track_log WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
