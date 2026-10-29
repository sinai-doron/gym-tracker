package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.doron.gymtracker.data.db.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLastCompleted(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL AND type = 'GYM' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getLatestUnfinishedGym(): SessionEntity?

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL AND startedAt < :beforeMillis")
    suspend fun getUnfinishedStartedBefore(beforeMillis: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NOT NULL AND startedAt >= :sinceMillis ORDER BY startedAt DESC")
    suspend fun getCompletedSince(sinceMillis: Long): List<SessionEntity>
}
