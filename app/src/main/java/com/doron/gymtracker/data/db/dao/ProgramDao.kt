package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.ProgramEntity

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE isActive = 1 ORDER BY rowid LIMIT 1")
    suspend fun getActive(): ProgramEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(program: ProgramEntity)
}
