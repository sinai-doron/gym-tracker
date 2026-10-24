package com.doron.gymtracker.domain.model

data class ExerciseEntry(
    val id: Long,
    val sessionId: Long,
    val exerciseId: String,
    val sets: List<SetRecord>
)
