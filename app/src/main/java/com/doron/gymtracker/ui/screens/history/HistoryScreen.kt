package com.doron.gymtracker.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.ui.components.SetLogRow

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val lastSession by viewModel.lastSession.collectAsState()
    val trends by viewModel.trends.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Last Session", modifier = Modifier.padding(vertical = 12.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Trends", modifier = Modifier.padding(vertical = 12.dp))
            }
        }

        when (selectedTab) {
            0 -> LastSessionTab(lastSession)
            1 -> TrendsTab(trends, onExerciseSelected = viewModel::selectExerciseForTrend)
        }
    }
}

@Composable
private fun LastSessionTab(state: LastSessionUiState) {
    if (state.workoutLabel == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sessions yet", style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "Workout ${state.workoutLabel?.name} — ${state.dateLabel ?: ""}",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(state.summaries) { _, summary ->
                Column {
                    Text(summary.exerciseName, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary)
                    summary.sets.forEachIndexed { i, set ->
                        SetLogRow(setNumber = i + 1, record = set)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendsTab(state: TrendsUiState, onExerciseSelected: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.selectedExercise ?: "Select exercise",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.exerciseNames.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onExerciseSelected(name); expanded = false }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (state.dataPoints.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                Text("No data yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LineChart(points = state.dataPoints, modifier = Modifier.fillMaxWidth().height(200.dp))
            Spacer(Modifier.height(8.dp))
            state.dataPoints.forEachIndexed { i, point ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(point.dateLabel, style = MaterialTheme.typography.bodyMedium)
                    Text("${point.value}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun LineChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }.coerceAtLeast(minV + 1f)
        val w = size.width
        val h = size.height
        val pts = points.mapIndexed { i, p ->
            Offset(
                x = (i.toFloat() / (points.size - 1)) * w,
                y = h - ((p.value - minV) / (maxV - minV)) * h
            )
        }
        for (i in 0 until pts.size - 1) {
            drawLine(primary, pts[i], pts[i + 1], strokeWidth = 4f)
        }
        pts.forEach { drawCircle(primary, radius = 8f, center = it) }
    }
}
