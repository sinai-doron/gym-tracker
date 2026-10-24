package com.doron.gymtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.ui.theme.SuccessGreen

@Composable
fun ExerciseCard(
    exercise: ExerciseDefinition,
    completedSets: List<SetRecord>,
    onClick: () -> Unit
) {
    val isDone = completedSets.size >= exercise.targetSets

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exercise.isOptional) 0.7f else 1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone)
                SuccessGreen.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (isDone) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(22.dp)
                    )
                } else if (exercise.isOptional) {
                    Text("אופציונלי",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                Text(
                    stringResource(R.string.sets_progress, completedSets.size, exercise.targetSets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDone) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (exercise.isPerSide && !isDone) {
                    Text("לכל צד",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (completedSets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(4.dp))
                completedSets.forEachIndexed { i, set ->
                    InlineSetRow(setNumber = i + 1, record = set)
                }
            }
        }
    }
}

@Composable
private fun InlineSetRow(setNumber: Int, record: SetRecord) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "סט $setNumber",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        val value = when (record) {
            is RepSetRecord -> {
                val w = if (record.weightKg == record.weightKg.toLong().toFloat())
                    record.weightKg.toLong().toString() else record.weightKg.toString()
                "${record.reps} × $w ק״ג"
            }
            is TimedSetRecord -> "${record.durationSecs}″"
            is DistanceSetRecord -> "${record.distanceM} מ׳"
        }
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary)
    }
}
