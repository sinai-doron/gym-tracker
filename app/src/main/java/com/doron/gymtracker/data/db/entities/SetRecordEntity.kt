package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "set_records")
data class SetRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val entryId: Long,
    val completedAt: Long,
    val type: String,           // "REP" or "TIMED"
    val reps: Int?,             // null for TIMED
    val weightKg: Float?,       // null for TIMED
    val durationSecs: Int?      // null for REP
)
