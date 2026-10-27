package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.LocationLogEntity

@Dao
interface LocationLogDao {
    @Insert
    suspend fun insert(entity: LocationLogEntity): Long

    @Query("SELECT * FROM location_log WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: Long): List<LocationLogEntity>

    @Query("DELETE FROM location_log WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
