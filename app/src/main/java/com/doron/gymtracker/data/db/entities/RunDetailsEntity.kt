package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** What a run or walk was set up with and the stats recorded for it, one row per cardio session. */
@Entity(tableName = "run_details")
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
    val weatherCondition: String?
)

/** A pause the runner asked for during a run; [endedAt] is null while it lasts. */
@Entity(tableName = "run_pauses", primaryKeys = ["sessionId", "startedAt"])
data class RunPauseEntity(
    val sessionId: Long,
    val startedAt: Long,
    val endedAt: Long?
)
