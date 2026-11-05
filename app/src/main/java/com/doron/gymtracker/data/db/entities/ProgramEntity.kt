package com.doron.gymtracker.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.doron.gymtracker.domain.model.Program

@Entity(tableName = "programs", indices = [Index(value = ["uuid"], unique = true)])
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String?,
    /** The first day of week 1, as days since 1970-01-01. */
    val startEpochDay: Long,
    val deloadEveryWeeks: Int?,
    val variantRule: String,        // NONE or ALTERNATE_WEEKLY
    val isActive: Boolean,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null,
    /** Archived by the user: kept, since its sessions point at its workouts. */
    val archivedAt: Long? = null,
    /** The training type it's for (`activity_types.id`); null is a strength programme. */
    val activityTypeId: String? = null,
    /** A deload week's weight, as a percentage of the usual one (version 19). */
    @ColumnInfo(defaultValue = "60") val deloadLoadPercent: Int = Program.DEFAULT_DELOAD_LOAD_PERCENT,
    /** A deload week's sets per exercise, at most the plan's (version 19). */
    @ColumnInfo(defaultValue = "2") val deloadSets: Int = Program.DEFAULT_DELOAD_SETS,
    /** How many weeks each side of the variant rule lasts (version 19). */
    @ColumnInfo(defaultValue = "1") val variantEveryWeeks: Int = 1
)
