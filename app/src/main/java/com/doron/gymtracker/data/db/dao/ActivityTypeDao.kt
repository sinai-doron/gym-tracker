package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.ActivityTypeEntity

@Dao
interface ActivityTypeDao {
    /** Every training type, hidden ones included, in the user's order. */
    @Query("SELECT * FROM activity_types ORDER BY sortOrder, id")
    suspend fun getAll(): List<ActivityTypeEntity>

    @Query("SELECT * FROM activity_types WHERE id = :id")
    suspend fun get(id: String): ActivityTypeEntity?
}
