package com.doron.gymtracker.domain.model

data class TrackLog(
    val id: Long,
    val sessionId: Long,
    val startedAt: Long,
    val title: String,
    val artist: String
)
