package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A training type: what a session is (strength, a run, Pilates…). Built-ins are known by a fixed id
 * (`strength`, `run`, `walk`) and named from the app's strings; a user's own get a uuid and the name
 * they typed. Built-ins can be hidden, never deleted, since sessions point at them.
 */
@Entity(tableName = "activity_types", indices = [Index(value = ["uuid"], unique = true)])
data class ActivityTypeEntity(
    @PrimaryKey val id: String,
    /** The name the user gave it; null shows a built-in's name in the phone's language. */
    val name: String? = null,
    /** Which of the app's icons it shows. */
    val icon: String,
    /** "#RRGGBB"; drives the History calendar and the week strip. */
    val color: String,
    /** Which screen records it: EXERCISES (the gym session), GPS (the run screen) or DURATION (a timer). */
    val recorder: String,
    /** The Health Connect exercise type it's written as; null when there is none. */
    val healthConnectType: Int? = null,
    /** For calories when the device gives none. */
    val metValue: Double? = null,
    val builtin: Boolean,
    val sortOrder: Int = 0,
    val hiddenAt: Long? = null,
    /** The id the server knows this row by (sync). Set by the database when left null. */
    val uuid: String? = null
)
