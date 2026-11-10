package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Indexed by session: a run's points are read by session, and without it every read scanned
// every point of every run (13 s to open a run, with years of runs imported)
@Entity(tableName = "location_log", indices = [Index("sessionId")])
data class LocationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Float?,
    val altitude: Double?,
    val accuracy: Float? = null,
    /** Direction of travel, degrees clockwise from north. */
    val bearing: Float? = null
)
