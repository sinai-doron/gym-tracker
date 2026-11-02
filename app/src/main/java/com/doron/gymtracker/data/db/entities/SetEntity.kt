package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One set of an exercise in a session (ADR-0006). What was done is in its parts: one for a normal
 * set, one per side when the sides are logged apart, one per segment of a drop set.
 */
@Entity(tableName = "sets", indices = [Index(value = ["uuid"], unique = true), Index("entryId")])
data class SetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val entryId: Long,
    /** Position within the exercise in this session, from 1. */
    val seq: Int,
    val kind: String = WORKING,     // WARMUP, WORKING, FAILURE, AMRAP
    val rpe: Float? = null,
    val note: String? = null,
    val completedAt: Long,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
) {
    companion object {
        const val WORKING = "WORKING"
    }
}

/**
 * What one part of a set recorded, in metric units. The five common metrics are columns, since
 * charts and records read them; rarer ones (height, speed, incline, level, band) go in [extra] as a
 * JSON object.
 */
@Entity(tableName = "set_parts", primaryKeys = ["setId", "seq"], indices = [Index("setId")])
data class SetPartEntity(
    val setId: Long,
    val seq: Int,
    /** LEFT or RIGHT when a side is logged on its own; null for both sides, or "each side". */
    val side: String? = null,
    val reps: Int? = null,
    /** As the exercise's load convention says: the total, per hand, added, or assistance. Null = no load. */
    val loadKg: Float? = null,
    val durationS: Int? = null,
    val distanceM: Int? = null,
    val calories: Int? = null,
    val extra: String? = null
)
