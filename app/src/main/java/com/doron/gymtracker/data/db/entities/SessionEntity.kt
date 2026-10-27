package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val type: String = "GYM",
    val distanceMeters: Double? = null
)
