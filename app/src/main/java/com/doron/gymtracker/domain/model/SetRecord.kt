package com.doron.gymtracker.domain.model

sealed interface SetRecord {
    val id: Long
    val entryId: Long
    val completedAt: Long
}

data class RepSetRecord(
    override val id: Long,
    override val entryId: Long,
    override val completedAt: Long,
    val reps: Int,
    val weightKg: Float
) : SetRecord

data class TimedSetRecord(
    override val id: Long,
    override val entryId: Long,
    override val completedAt: Long,
    val durationSecs: Int
) : SetRecord

data class DistanceSetRecord(
    override val id: Long,
    override val entryId: Long,
    override val completedAt: Long,
    val distanceM: Int
) : SetRecord
