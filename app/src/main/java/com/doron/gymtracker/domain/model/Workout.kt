package com.doron.gymtracker.domain.model

data class Workout(
    val id: String,
    /** The short code on the workout's chip: A, B, Push… */
    val code: String,
    val exercises: List<ExerciseDefinition>,
    /** The name the user gave it; null shows the built-in name of the seeded plan's workouts. */
    val name: String? = null,
    /** Its place in the programme's rotation. */
    val sortOrder: Int = 0
)
