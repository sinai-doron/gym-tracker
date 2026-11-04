package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workouts", indices = [Index(value = ["uuid"], unique = true), Index("programId")])
data class WorkoutEntity(
    @PrimaryKey val id: String,
    /** The short code on the workout's chip (A, B, Push…). */
    val label: String,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null,
    val programId: String,
    /** Its place in the programme's rotation. */
    val sortOrder: Int = 0,
    /** The name the user gave it; null shows the seeded plan's built-in name. */
    val name: String? = null,
    /** 1 (Monday) to 7 (Sunday) when the workout is tied to a weekday; not used yet. */
    val weekday: Int? = null,
    /** Removed by the user: kept, so history still shows it, and left out of the rotation. */
    val hiddenAt: Long? = null
)
