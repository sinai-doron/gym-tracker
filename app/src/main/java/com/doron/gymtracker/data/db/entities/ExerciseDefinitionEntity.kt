package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_definitions")
data class ExerciseDefinitionEntity(
    @PrimaryKey val id: String,
    val workoutId: String,
    val name: String,
    val type: String,           // "REP", "TIMED", or "DISTANCE"
    val targetSets: Int,
    val restDurationSecs: Int,
    val targetReps: Int?,       // REP only
    val targetDistanceM: Int?,  // DISTANCE only
    val isOptional: Boolean = false,
    val isPerSide: Boolean = false
)
