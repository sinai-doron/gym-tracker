package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "set_records", indices = [Index(value = ["uuid"], unique = true)])
data class SetRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val entryId: Long,
    val completedAt: Long,
    val type: String,       // "REP", "TIMED", or "DISTANCE"
    val reps: Int?,
    val weightKg: Float?,
    val durationSecs: Int?,
    val distanceM: Int?,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
