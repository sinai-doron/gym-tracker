package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "routes", indices = [Index(value = ["uuid"], unique = true)])
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val distanceMeters: Double,
    val createdAt: Long,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)

@Entity(tableName = "route_points", primaryKeys = ["routeId", "seq"], indices = [Index("routeId")])
data class RoutePointEntity(
    val routeId: Long,
    val seq: Int,
    val latitude: Double,
    val longitude: Double,
    val elevationM: Double?
)
