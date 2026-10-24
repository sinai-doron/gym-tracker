package com.doron.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord

@Composable
fun SetLogRow(setNumber: Int, record: SetRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Set $setNumber", style = MaterialTheme.typography.bodyLarge)
        val label = when (record) {
            is RepSetRecord -> "${record.reps} reps × ${record.weightKg}kg"
            is TimedSetRecord -> "${record.durationSecs}s"
        }
        Text(label, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary)
    }
}
