package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val uuid: String? = null
)
