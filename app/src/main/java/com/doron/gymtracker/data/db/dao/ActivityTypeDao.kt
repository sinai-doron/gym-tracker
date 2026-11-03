package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doron.gymtracker.data.db.entities.ActivityTypeEntity

@Dao
interface ActivityTypeDao {
    /** Every training type, hidden ones included, in the user's order. */
    @Query("SELECT * FROM activity_types ORDER BY sortOrder, id")
    suspend fun getAll(): List<ActivityTypeEntity>

    @Query("SELECT * FROM activity_types WHERE id = :id")
    suspend fun get(id: String): ActivityTypeEntity?

    /** Adds the built-ins a phone doesn't have yet; ones it has keep the user's changes. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(types: List<ActivityTypeEntity>)

    @Insert
    suspend fun insert(type: ActivityTypeEntity)

    @Query("UPDATE activity_types SET hiddenAt = :at WHERE id = :id")
    suspend fun setHidden(id: String, at: Long?)

    @Query("UPDATE activity_types SET name = :name, icon = :icon WHERE id = :id AND builtin = 0")
    suspend fun edit(id: String, name: String, icon: String)

    @Query("SELECT coalesce(max(sortOrder), 0) FROM activity_types")
    suspend fun maxSortOrder(): Int

    @Query("SELECT count(*) FROM sessions WHERE activityTypeId = :id")
    suspend fun sessionCount(id: String): Int

    @Query("DELETE FROM activity_types WHERE id = :id AND builtin = 0")
    suspend fun deleteOwn(id: String)
}
