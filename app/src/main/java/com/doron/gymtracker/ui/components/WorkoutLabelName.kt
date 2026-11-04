package com.doron.gymtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.Workout

/** The built-in name of the seeded plan's workouts, translated; null for any other workout. */
@StringRes
fun Workout.defaultNameRes(): Int? = when (id) {
    "workout_a" -> R.string.session_workout_a
    "workout_b" -> R.string.session_workout_b
    "workout_c" -> R.string.session_workout_c
    else -> null
}

/**
 * The name the user gave the workout, else its built-in name, else its code: a workout made
 * before names took the code's place may have only the code.
 */
@Composable
fun Workout.localizedName(): String = name ?: defaultNameRes()?.let { stringResource(it) } ?: code
