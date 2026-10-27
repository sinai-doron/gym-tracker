package com.doron.gymtracker.domain.model

data class Session(
    val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    val type: String = "GYM",
    val distanceMeters: Double? = null
)
