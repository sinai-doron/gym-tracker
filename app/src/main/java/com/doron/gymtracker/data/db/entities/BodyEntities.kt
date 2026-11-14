package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** One weigh-in (version 21), kept in kg whatever unit the screens show. Syncs as `weight`. */
@Entity(tableName = "weight_entries", indices = [Index(value = ["uuid"], unique = true), Index("measuredAt")])
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val measuredAt: Long,
    val kg: Double,
    val uuid: String? = null
)

/** The weight goal, one row (id 1). Syncs as `weight_goal` under a fixed id: one per user. */
@Entity(tableName = "weight_goal", indices = [Index(value = ["uuid"], unique = true)])
data class WeightGoalEntity(
    @PrimaryKey val id: Int = 1,
    val startKg: Double,
    val startAt: Long,
    val targetKg: Double,
    val paceKgPerWeek: Double,
    val uuid: String? = WEIGHT_GOAL_UUID
)

/** One drink (version 21), in ml. Syncs as `water`. */
@Entity(tableName = "water_entries", indices = [Index(value = ["uuid"], unique = true), Index("drankAt")])
data class WaterEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drankAt: Long,
    val ml: Int,
    val uuid: String? = null
)

/** The water goal and the quick-add sizes, one row (id 1). Syncs as `water_goal` under a fixed id. */
@Entity(tableName = "water_goal", indices = [Index(value = ["uuid"], unique = true)])
data class WaterGoalEntity(
    @PrimaryKey val id: Int = 1,
    val dailyMl: Int,
    val glassMl: Int,
    val bottleMl: Int,
    val uuid: String? = WATER_GOAL_UUID
)

/** The same on every phone and for every user: the server keeps one goal per user under it. */
val WEIGHT_GOAL_UUID: String = UUID.nameUUIDFromBytes("gym-tracker:weight-goal".toByteArray()).toString()
val WATER_GOAL_UUID: String = UUID.nameUUIDFromBytes("gym-tracker:water-goal".toByteArray()).toString()
