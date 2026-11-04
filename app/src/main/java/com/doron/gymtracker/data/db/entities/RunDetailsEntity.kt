package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** What a run or walk was set up with and the stats recorded for it, one row per cardio session. */
@Entity(tableName = "run_details", indices = [Index(value = ["uuid"], unique = true)])
data class RunDetailsEntity(
    @PrimaryKey val sessionId: Long,
    val targetType: String,
    /** Kilometers for a distance target, minutes for a duration target. */
    val targetValue: Double?,
    val lapMeters: Int,
    /** The phone's step counter when the run started; steps are counted from it. */
    val stepCounterAtStart: Long?,
    val steps: Int?,
    val elevationGainM: Double?,
    val calories: Int?,
    val weatherTempC: Int?,
    val weatherCondition: String?,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)

/**
 * A run's steps so far at a moment while it was recorded, for its cadence over the run and by
 * lap. Never synced: it stays on the phone, like the step counter's baseline.
 */
@Entity(tableName = "run_steps", primaryKeys = ["sessionId", "timestamp"])
data class RunStepEntity(
    val sessionId: Long,
    val timestamp: Long,
    val steps: Int
)

/** A pause the runner asked for during a run; [endedAt] is null while it lasts. */
@Entity(tableName = "run_pauses", primaryKeys = ["sessionId", "startedAt"], indices = [Index(value = ["uuid"], unique = true)])
data class RunPauseEntity(
    val sessionId: Long,
    val startedAt: Long,
    val endedAt: Long?,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
