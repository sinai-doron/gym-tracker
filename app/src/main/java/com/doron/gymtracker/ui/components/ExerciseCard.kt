package com.doron.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.ui.theme.SuccessGreen
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

@Composable
fun ExerciseCard(
    exercise: ExerciseDefinition,
    completedSets: List<SetRecord>,
    onClick: () -> Unit,
    /** Left out of today's session: faded, and not counted as unfinished. */
    skipped: Boolean = false,
    /** Skips it for today (true) or takes it back (false); no button without it. */
    onSkip: ((Boolean) -> Unit)? = null
) {
    val isDone = completedSets.size >= exercise.targetSets
    // Skipped: the card greys out, all but its button, which brings it back
    val greyed = skipped && !isDone
    val content = Modifier.alpha(if (greyed) 0.45f else 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exercise.isOptional && !isDone && !skipped) 0.7f else 1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (greyed) AppColors.ground else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone || greyed) 0.dp else 2.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            if (isDone) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.small)
                        .background(SuccessGreen)
                )
            }
            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                val skippedLabel = stringResource(R.string.session_skipped)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        exercise.localizedName(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = content.weight(1f)
                    )
                    if (isDone) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (exercise.isOptional && !skipped) {
                        Text(
                            stringResource(R.string.session_optional),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Skip it for today from the list without opening it; the same button takes it back
                    if (onSkip != null && !isDone) {
                        Spacer(Modifier.width(8.dp))
                        SquareIconButton(
                            // Skip; once skipped, undo (it mirrors itself right to left)
                            icon = if (skipped) R.drawable.ic_undo else R.drawable.ic_skip,
                            description = stringResource(if (skipped) R.string.session_undo_skip else R.string.session_skip_today),
                            size = AppShapes.rowIconButton,
                            radius = AppShapes.rowIconButtonRadius,
                            iconSize = AppShapes.rowIconButtonIcon,
                            background = if (skipped) AppColors.surface else AppColors.ground,
                            tint = if (skipped) AppColors.accentStrength else AppColors.textMuted,
                            border = if (skipped) AppColors.borderStrong else null,
                            onClick = { onSkip(!skipped) },
                            modifier = Modifier.semantics { stateDescription = if (skipped) skippedLabel else "" }
                        )
                    }
                }
                Row(modifier = content) {
                    if (isDone) {
                        Text(
                            pluralStringResource(R.plurals.session_done_sets, completedSets.size, completedSets.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen,
                            modifier = Modifier.weight(1f)
                        )
                    } else if (greyed) {
                        Text(
                            stringResource(R.string.session_skipped),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            stringResource(R.string.sets_progress, completedSets.size, exercise.targetSets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        if (exercise.isPerSide) {
                            Text(
                                stringResource(R.string.session_per_side),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (completedSets.isNotEmpty() && !isDone) {
                    Spacer(Modifier.height(10.dp))
                    Column(modifier = content) {
                        completedSets.forEachIndexed { i, set ->
                            InlineSetRow(setNumber = i + 1, record = set)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineSetRow(setNumber: Int, record: SetRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "$setNumber",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(10.dp))
        val value = when (record) {
            is RepSetRecord -> {
                val w = weightText(record.weightKg)
                if (record.weightKg == 0f) pluralStringResource(R.plurals.session_reps, record.reps, record.reps)
                else stringResource(R.string.session_reps_times_weight_unit, record.reps, w)
            }
            is TimedSetRecord -> formatDuration(record.durationSecs)
            is DistanceSetRecord -> stringResource(R.string.meters_value, record.distanceM)
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatDuration(totalSecs: Int): String {
    val m = totalSecs / 60
    val s = totalSecs % 60
    return "%d:%02d".format(m, s)
}
