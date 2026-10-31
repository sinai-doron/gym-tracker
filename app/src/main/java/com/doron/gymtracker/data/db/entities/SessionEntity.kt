package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions", indices = [Index(value = ["uuid"], unique = true)])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val type: String = "GYM",
    val distanceMeters: Double? = null,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
