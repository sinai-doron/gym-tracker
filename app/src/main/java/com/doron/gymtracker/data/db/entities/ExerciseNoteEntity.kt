package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The note a user keeps on an exercise (version 21): seat height, grip, the machine's number. One
 * per library exercise, so it follows the exercise into any programme. It syncs as an
 * `exercise_note` known by the exercise's id, which is also its [uuid]: a built-in's id is the
 * same on every phone, so two phones editing it update one note.
 */
@Entity(tableName = "exercise_notes", indices = [Index(value = ["uuid"], unique = true)])
data class ExerciseNoteEntity(
    /** The library exercise (`exercises.id`). */
    @PrimaryKey val exerciseId: String,
    val text: String,
    val uuid: String? = exerciseId
)
