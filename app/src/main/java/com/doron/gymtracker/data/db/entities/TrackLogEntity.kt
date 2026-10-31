package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "track_log", indices = [Index(value = ["uuid"], unique = true)])
data class TrackLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startedAt: Long,
    val title: String,
    val artist: String,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
