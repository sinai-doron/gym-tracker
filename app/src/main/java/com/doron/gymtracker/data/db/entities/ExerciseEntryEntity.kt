package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_entries", indices = [Index(value = ["uuid"], unique = true)])
data class ExerciseEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val sessionId: Long,
    val exerciseId: String,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
