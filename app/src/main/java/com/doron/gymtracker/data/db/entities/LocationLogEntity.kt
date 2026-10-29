package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_log")
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
