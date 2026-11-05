package com.doron.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * The next workout: its name under a dumbbell tile, a button to pick another, chips for when it
 * was last done, its exercises and its usual length, then Start and a button that unfolds the
 * exercise list.
 */
@Composable
fun NextWorkoutCard(
    preview: WorkoutPreview,
    /** The day it falls on, when the programme follows the week. */
    day: LocalDate?,
    onStart: () -> Unit,
    onSwap: () -> Unit,
    onExerciseClick: (exerciseId: String) -> Unit
) {
    // Folded again whenever home is opened or the next workout changes
    var showExercises by remember(preview.workout.id) { mutableStateOf(false) }
    DesignCard {
        DesignCardHeader(
            icon = R.drawable.ic_home_dumbbell,
            tile = AppColors.tintStrength,
            iconColor = AppColors.accentStrength,
            label = nextWorkoutLabel(day),
            title = preview.workout.localizedName(),
            action = {
                SquareIconButton(
                    icon = R.drawable.ic_home_swap,
                    description = stringResource(R.string.home_swap_workout),
                    size = AppShapes.cardIconButton,
                    radius = AppShapes.cardIconButtonRadius,
                    iconSize = AppShapes.cardIconButtonIcon,
                    background = AppColors.ground,
                    tint = AppColors.textSecondary,
                    onClick = onSwap
                )
            }
        )

        // A Row, not the design's wrapping row: FlowRow fails at runtime on this branch's Compose
        // versions (fixed by #14), and the three chips fit on one line
        Row(horizontalArrangement = Arrangement.spacedBy(AppShapes.chipGap)) {
            if (preview.lastDoneAt == null) {
                DesignChip(stringResource(R.string.never_done), AppColors.warnTint, AppColors.warnText, dot = AppColors.warnDot)
            } else {
                DesignChip(lastDoneText(preview.lastDoneAt), AppColors.ground, AppColors.textSecondary)
            }
            DesignChip(stringResource(R.string.exercises_count, preview.exercises.size), AppColors.ground, AppColors.textSecondary)
            preview.typicalMinutes?.let {
                DesignChip(stringResource(R.string.typical_minutes, it), AppColors.ground, AppColors.textSecondary)
            }
        }

        DesignButtonRow {
            DesignButton(
                text = stringResource(R.string.start_session),
                icon = R.drawable.ic_home_play,
                background = AppColors.accentStrength,
                color = AppColors.surface,
                onClick = onStart,
                modifier = Modifier.weight(1f)
            )
            SquareIconButton(
                icon = R.drawable.ic_home_list,
                description = if (showExercises) stringResource(R.string.home_hide_exercises)
                else stringResource(R.string.home_show_exercises, preview.exercises.size),
                size = AppShapes.sideIconButton,
                radius = AppShapes.buttonRadius,
                iconSize = AppShapes.sideIconButtonIcon,
                // Tinted while the list is open
                background = if (showExercises) AppColors.tintStrength else AppColors.surface,
                tint = AppColors.accentStrength,
                border = AppColors.borderStrong,
                onClick = { showExercises = !showExercises }
            )
        }

        if (showExercises) {
            ExercisePreviewList(preview.exercises, onExerciseClick = onExerciseClick)
        }
    }
}

/** Cardio: Run filled, Walk tinted, side by side; either one alone when the other is hidden. */
@Composable
fun CardioCard(onRun: (() -> Unit)?, onWalk: (() -> Unit)?) {
    DesignCard(shadow = AppShapes.cardioCardShadow) {
        DesignCardHeader(
            icon = R.drawable.ic_activity_run,
            tile = AppColors.tintCardio,
            iconColor = AppColors.accentCardio,
            label = null,
            title = stringResource(R.string.home_cardio_title)
        )
        DesignButtonRow {
            onRun?.let {
                DesignButton(
                    text = stringResource(R.string.home_run),
                    icon = R.drawable.ic_activity_run,
                    background = AppColors.accentCardio,
                    color = AppColors.surface,
                    onClick = it,
                    modifier = Modifier.weight(1f)
                )
            }
            onWalk?.let {
                DesignButton(
                    text = stringResource(R.string.home_walk),
                    icon = R.drawable.ic_activity_walk,
                    background = AppColors.tintCardio,
                    color = AppColors.cardioTextOnTint,
                    onClick = it,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** "Next workout"; by weekday "Today's workout", "Tomorrow's workout" or "Next workout · Monday". */
@Composable
private fun nextWorkoutLabel(day: LocalDate?): String {
    val today = LocalDate.now()
    return when (day) {
        null -> stringResource(R.string.next_workout)
        today -> stringResource(R.string.next_workout_today)
        today.plusDays(1) -> stringResource(R.string.next_workout_tomorrow)
        else -> stringResource(R.string.next_workout_on, day.dayOfWeek.getDisplayName(TextStyle.FULL, appLocale()))
    }
}
