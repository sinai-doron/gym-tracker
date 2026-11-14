package com.doron.gymtracker.domain.model

data class ExerciseEntry(
    val id: Long,
    val sessionId: Long,
    val exerciseId: String,
    val sets: List<SetRecord>,
    /** Left out of this session on purpose. */
    val skipped: Boolean = false,
    /** A note on this session's sets. */
    val note: String? = null
)
