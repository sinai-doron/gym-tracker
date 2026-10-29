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
    val isPerSide: Boolean = false,
    val isBodyweight: Boolean = false,
    /** Set on a week-B variant row: the id of the week-A exercise it alternates with. */
    val variantOfId: String? = null,
    /** Position within the workout; the user can reorder exercises. */
    val sortOrder: Int = 0,
    /** Removed by the user. Kept (not deleted) so history stays and the plan sync won't re-add it. */
    val isRemoved: Boolean = false
)
