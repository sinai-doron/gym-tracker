package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_records")
data class SetRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val entryId: Long,
    val completedAt: Long,
    val type: String,       // "REP", "TIMED", or "DISTANCE"
    val reps: Int?,
    val weightKg: Float?,
    val durationSecs: Int?,
    val distanceM: Int?
)
