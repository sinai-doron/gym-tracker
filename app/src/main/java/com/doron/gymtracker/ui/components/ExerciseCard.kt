package com.doron.gymtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.ui.theme.SuccessGreen
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord

@Composable
fun ExerciseCard(
    exercise: ExerciseDefinition,
    completedSets: List<SetRecord>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (exercise.isOptional) 0.7f else 1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                Text(exercise.name, style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f))
                if (exercise.isOptional) {
                    Text("אופציונלי",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row {
                Text(
                    stringResource(R.string.sets_progress, completedSets.size, exercise.targetSets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (completedSets.size >= exercise.targetSets)
                        SuccessGreen
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (exercise.isPerSide) {
                    Text("לכל צד",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (completedSets.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("סט $setNumber",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        val value = when (record) {
            is RepSetRecord -> "${record.reps} × ${record.weightKg} ק״ג"
            is TimedSetRecord -> "${record.durationSecs}″"
            is DistanceSetRecord -> "${record.distanceM} מ׳"
        }
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}
