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

/** The name the user gave the workout, else its built-in name, else its code. */
@Composable
fun Workout.localizedName(): String = name ?: defaultNameRes()?.let { stringResource(it) } ?: code

/**
 * What goes in a workout's circle: its code when it is short (A, B, UA, P2), else nothing. A code
 * that is a word ("Push", "דחיפה") doesn't fit, and the name beside the circle says it already.
 */
fun circleCode(code: String): String = code.takeIf { it.length <= MAX_CIRCLE_CODE }.orEmpty()

private const val MAX_CIRCLE_CODE = 3
