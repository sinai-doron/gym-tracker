package com.doron.gymtracker.ui.screens.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.ProgressMetric
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.ui.theme.Purple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    onBack: () -> Unit,
    viewModel: ExerciseProgressViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val progress = state.progress

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(progress?.exercise?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
            return@Scaffold
        }
        if (progress == null || progress.points.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_data_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val formatValue = metricFormatter(progress.metric)
        val shortDate = remember { SimpleDateFormat("dd.MM", Locale.getDefault()) }
        val fullDate = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Personal record: the headline number
            progress.best?.let { best ->
                item {
                    Card {
                        Text(
                            stringResource(R.string.personal_record),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatValue(best.value),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            fullDate.format(Date(best.date)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card {
                    Text(
                        stringResource(metricTitle(progress.metric)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    ProgressChart(
                        points = progress.points,
                        formatValue = formatValue,
                        formatDate = { shortDate.format(Date(it)) }
                    )
                }
            }
            // The same data as a table, newest first
            items(progress.sessions, key = { it.sessionId }) { session ->
                SessionRow(session, fullDate.format(Date(session.startedAt)))
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun SessionRow(session: ExerciseSessionSets, date: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            date,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            // LTR isolate keeps "10×40" readable inside RTL text
            session.sets.joinToString("  ·  ") { "⁦${it.short()}⁩" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun SetRecord.short(): String = when (this) {
    is RepSetRecord -> if (weightKg == 0f) "$reps" else "$reps×${formatNumber(weightKg)}"
    is TimedSetRecord -> "%d:%02d".format(durationSecs / 60, durationSecs % 60)
    is DistanceSetRecord -> "${distanceM}m"
}

@Composable
private fun metricFormatter(metric: ProgressMetric): (Float) -> String {
    val res = LocalContext.current.resources
    return when (metric) {
        ProgressMetric.WEIGHT -> { v -> res.getString(R.string.weight_value, formatNumber(v)) }
        ProgressMetric.REPS -> { v -> res.getString(R.string.reps_value, v.toInt()) }
        ProgressMetric.DURATION -> { v -> "%d:%02d".format(v.toInt() / 60, v.toInt() % 60) }
        ProgressMetric.DISTANCE -> { v -> res.getString(R.string.distance_value, v.toInt()) }
    }
}

private fun metricTitle(metric: ProgressMetric): Int = when (metric) {
    ProgressMetric.WEIGHT -> R.string.metric_heaviest_weight
    ProgressMetric.REPS -> R.string.metric_most_reps
    ProgressMetric.DURATION -> R.string.metric_longest_set
    ProgressMetric.DISTANCE -> R.string.metric_longest_distance
}

private fun formatNumber(v: Float): String =
    if (v == v.toLong().toFloat()) v.toLong().toString() else v.toString()
