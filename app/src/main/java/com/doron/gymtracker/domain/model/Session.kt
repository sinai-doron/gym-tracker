package com.doron.gymtracker.domain.model

data class Session(
    val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?
)
