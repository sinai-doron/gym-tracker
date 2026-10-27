package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.doron.gymtracker.data.db.entities.SetRecordEntity

@Dao
interface SetRecordDao {
    @Insert
    suspend fun insert(record: SetRecordEntity): Long

    @Update
    suspend fun update(record: SetRecordEntity)

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
}
