package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.doron.gymtracker.data.db.entities.WaterEntryEntity
import com.doron.gymtracker.data.db.entities.WaterGoalEntity
import com.doron.gymtracker.data.db.entities.WeightEntryEntity
import com.doron.gymtracker.data.db.entities.WeightGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    /** Newest first. */
    @Query("SELECT * FROM weight_entries ORDER BY measuredAt DESC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Query("SELECT * FROM weight_entries ORDER BY measuredAt DESC LIMIT 1")
    suspend fun latest(): WeightEntryEntity?

    @Query("SELECT * FROM weight_entries WHERE id = :id")
    suspend fun get(id: Long): WeightEntryEntity?

    @Query("SELECT count(*) FROM weight_entries WHERE measuredAt >= :from AND measuredAt < :until")
    suspend fun countBetween(from: Long, until: Long): Int

    @Insert
    suspend fun insert(entry: WeightEntryEntity): Long

    @Update
    suspend fun update(entry: WeightEntryEntity)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM weight_goal WHERE id = 1")
    fun observeGoal(): Flow<WeightGoalEntity?>

    @Upsert
    suspend fun upsertGoal(goal: WeightGoalEntity)

    @Query("DELETE FROM weight_goal")
    suspend fun deleteGoal()
}

@Dao
interface WaterDao {
    /** Since [from], newest first. */
    @Query("SELECT * FROM water_entries WHERE drankAt >= :from ORDER BY drankAt DESC")
    fun observeSince(from: Long): Flow<List<WaterEntryEntity>>

    @Query("SELECT coalesce(sum(ml), 0) FROM water_entries WHERE drankAt >= :from AND drankAt < :until")
    suspend fun totalBetween(from: Long, until: Long): Int

    @Insert
    suspend fun insert(entry: WaterEntryEntity): Long

    @Query("DELETE FROM water_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM water_goal WHERE id = 1")
    fun observeGoal(): Flow<WaterGoalEntity?>

    @Query("SELECT * FROM water_goal WHERE id = 1")
    suspend fun goal(): WaterGoalEntity?

    @Upsert
    suspend fun upsertGoal(goal: WaterGoalEntity)
}
