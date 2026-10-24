package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_definitions")
data class ExerciseDefinitionEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val name: String,
    val type: String,       // "REP" or "TIMED"
    val targetSets: Int,
    val restDurationSecs: Int,
    val targetReps: Int?    // null for TIMED exercises
)
