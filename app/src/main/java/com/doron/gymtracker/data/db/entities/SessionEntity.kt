package com.doron.gymtracker.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.doron.gymtracker.data.db.ActivityTypeMigration

@Entity(tableName = "sessions", indices = [Index(value = ["uuid"], unique = true), Index("activityTypeId")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val workoutId: String,
    val startedAt: Long,
    val finishedAt: Long?,
    /** Which screen recorded it: GYM, RUNNING or WALKING; OTHER for a timer or a session logged by hand. */
    val type: String = "GYM",
    val distanceMeters: Double? = null,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null,
    /** Its training type (`activity_types.id`); a session the gym or run screen made is Strength, Run or Walk. */
    val activityTypeId: String = ActivityTypeMigration.forSessionType(type),
    /** Entered afterwards rather than recorded live. */
    @ColumnInfo(defaultValue = "0")
    val loggedManually: Boolean = false,
    /** Set for a session logged by hand or timed; a recorded one's runs from [startedAt] to [finishedAt]. */
    val durationS: Int? = null,
    val calories: Int? = null,
    val note: String? = null
)
