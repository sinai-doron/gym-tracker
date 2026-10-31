package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "workouts", indices = [Index(value = ["uuid"], unique = true)])
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val label: String,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
