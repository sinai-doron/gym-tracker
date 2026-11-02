package com.doron.gymtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.WorkoutLabel

@StringRes
fun WorkoutLabel.nameRes(): Int = when (this) {
    WorkoutLabel.A -> R.string.session_workout_a
    WorkoutLabel.B -> R.string.session_workout_b
    WorkoutLabel.C -> R.string.session_workout_c
}

@Composable
fun WorkoutLabel.localizedName(): String = stringResource(nameRes())
