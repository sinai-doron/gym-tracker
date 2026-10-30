package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.doron.gymtracker.data.db.entities.RunDetailsEntity
import com.doron.gymtracker.data.db.entities.RunPauseEntity

@Dao
interface RunDetailsDao {
    @Upsert
    suspend fun upsert(entity: RunDetailsEntity)

    @Query("SELECT * FROM run_details WHERE sessionId = :sessionId")
    suspend fun get(sessionId: Long): RunDetailsEntity?

    @Query("DELETE FROM run_details WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Upsert
    suspend fun upsertPause(entity: RunPauseEntity)

    @Query("SELECT * FROM run_pauses WHERE sessionId = :sessionId ORDER BY startedAt")
    suspend fun getPauses(sessionId: Long): List<RunPauseEntity>

    @Query("DELETE FROM run_pauses WHERE sessionId = :sessionId")
    suspend fun deletePauses(sessionId: Long)
}
