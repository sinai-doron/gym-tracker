package com.doron.gymtracker.domain.model

/** A point of a planned route. */
data class RoutePoint(val latitude: Double, val longitude: Double, val elevationM: Double? = null)

/** A route to run, from a past run or an imported GPX file. */
data class SavedRoute(
    val id: Long,
    val name: String,
    val distanceMeters: Double,
    val createdAt: Long,
    val points: List<RoutePoint> = emptyList()
)
