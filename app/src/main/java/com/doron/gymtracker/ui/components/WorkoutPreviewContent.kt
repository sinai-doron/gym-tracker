package com.doron.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExercisePreview
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.ui.theme.Purple
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Chips: when the workout was last done, how long it usually takes, how many exercises. */
@Composable
fun WorkoutMetaRow(preview: WorkoutPreview, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetaChip(lastDoneText(preview.lastDoneAt))
        preview.typicalMinutes?.let { MetaChip(stringResource(R.string.typical_minutes, it)) }
        MetaChip(stringResource(R.string.exercises_count, preview.exercises.size))
    }
}

@Composable
fun ExercisePreviewList(
    exercises: List<ExercisePreview>,
    modifier: Modifier = Modifier,
    onExerciseClick: ((exerciseId: String) -> Unit)? = null
) {
    // Reserve the weight column on every row so the sets column stays aligned
    val showWeights = exercises.any { it.lastWeightKg != null }
    Column(modifier = modifier) {
        exercises.forEachIndexed { i, item ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ExercisePreviewRow(item, showWeights, onExerciseClick)
        }
    }
}

@Composable
private fun ExercisePreviewRow(
    item: ExercisePreview,
    showWeights: Boolean,
    onClick: ((exerciseId: String) -> Unit)?
) {
    val exercise = item.exercise
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { m -> if (onClick != null) m.clickable { onClick(exercise.id) } else m }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            exercise.name,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            when (exercise) {
                // LTR isolate: in RTL, "4×6" would otherwise render as "6×4"
                is RepExerciseDefinition -> "\u2066${exercise.targetSets}×${exercise.targetReps}\u2069"
                is DistanceExerciseDefinition -> "\u2066${exercise.targetSets}×\u2069" +
                    stringResource(R.string.distance_value, exercise.targetDistanceM)
                is TimedExerciseDefinition -> stringResource(R.string.sets_count, exercise.targetSets)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showWeights) {
            Text(
                item.lastWeightKg?.let { stringResource(R.string.weight_value, formatKg(it)) } ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Purple,
                textAlign = TextAlign.End,
                modifier = Modifier.width(72.dp)
            )
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = Purple,
        modifier = Modifier
            .background(Purple.copy(alpha = 0.10f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun lastDoneText(lastDoneAt: Long?): String {
    if (lastDoneAt == null) return stringResource(R.string.never_done)
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(lastDoneAt).atZone(zone).toLocalDate()
    return when (val days = ChronoUnit.DAYS.between(day, LocalDate.now(zone)).toInt()) {
        0 -> stringResource(R.string.done_today)
        1 -> stringResource(R.string.done_yesterday)
        else -> stringResource(R.string.done_days_ago, days)
    }
}

private fun formatKg(kg: Float): String =
    if (kg == kg.toLong().toFloat()) kg.toLong().toString() else kg.toString()
