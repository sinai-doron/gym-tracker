package com.doron.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord

@Composable
fun SetLogRow(setNumber: Int, record: SetRecord, isEditing: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (isEditing) it.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)) else it }
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("סט $setNumber", style = MaterialTheme.typography.bodyLarge)
        val label = when (record) {
            is RepSetRecord -> {
                val w = if (record.weightKg == record.weightKg.toLong().toFloat())
                    record.weightKg.toLong().toString() else record.weightKg.toString()
                "${record.reps} חזרות × $w ק״ג"
            }
            is TimedSetRecord -> {
                val m = record.durationSecs / 60
                val s = record.durationSecs % 60
                "%d:%02d".format(m, s)
            }
            is DistanceSetRecord -> "${record.distanceM} מטרים"
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}
