package com.doron.gymtracker.data.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An exercise in the library (ADR-0006): what a set of it records and how its load counts. Built-in
 * exercises come from the catalogue (assets/exercises/exercises.json) and are known by [slug] and an
 * id derived from it, the same on every phone; a user's own exercises will be `CUSTOM`.
 */
@Entity(tableName = "exercises", indices = [Index(value = ["slug"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val slug: String?,
    val source: String,             // "BUILTIN" or "CUSTOM"
    /** What a set records: reps_load, reps, reps_assisted, time, distance_load, distance_time, calories_time. */
    val preset: String,
    /** How the load counts: total, per_hand, added, assistance, none. */
    val load: String,
    /** kg, lb or plates; null follows the Settings unit. */
    val loadUnit: String? = null,
    val sides: String,              // "both" or "per_side"
    val equipment: String,
    val mechanics: String?,         // "compound" or "isolation"
    val weightStepKg: Double?,
    /** The catalogue version a built-in was last written from; null for a user's own. */
    val catalogueVersion: Int?,
    /** Gone from the catalogue, or hidden by the user. Kept so history still finds it. */
    val archivedAt: Long? = null
)

/** An exercise's name in one language, or a search alias for it ("bench", "בנץ'"). */
@Entity(tableName = "exercise_names", primaryKeys = ["exerciseId", "lang", "name"], indices = [Index("name")])
data class ExerciseNameEntity(
    val exerciseId: String,
    val lang: String,
    val name: String,
    val isAlias: Boolean
)

/** A muscle an exercise trains, from the catalogue's 18 (vocabulary.json). */
@Entity(tableName = "exercise_muscles", primaryKeys = ["exerciseId", "muscle"], indices = [Index("muscle")])
data class ExerciseMuscleEntity(
    val exerciseId: String,
    val muscle: String,
    val isPrimary: Boolean
)
