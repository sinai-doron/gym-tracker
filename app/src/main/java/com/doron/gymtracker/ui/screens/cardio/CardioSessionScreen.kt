package com.doron.gymtracker.ui.screens.cardio

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.ui.components.MusicControls
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen

@Composable
fun CardioSessionScreen(
    viewModel: CardioSessionViewModel,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }

    if (state.isFinished) {
        CardioSummary(state = state, onDone = onFinished)
        return
    }

    // Leaving with BACK used to leave the session open forever; ask instead
    BackHandler { showFinishDialog = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            if (state.type == "RUNNING") "🏃 ריצה" else "🚶 הליכה",
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(40.dp))

        // Timer - big center display
        Text(
            formatTime(state.elapsedSecs),
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp
            ),
            color = Purple
        )

        Spacer(Modifier.height(32.dp))

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                label = "מרחק",
                value = "%.2f ק״מ".format(state.distanceMeters / 1000.0),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "קצב",
                value = if (state.currentPace.isNotEmpty()) "${state.currentPace} /ק״מ" else "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                label = "מהירות",
                value = if (state.currentSpeed > 0.3f) "%.1f קמ״ש".format(state.currentSpeed * 3.6f) else "—",
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "נקודות GPS",
                value = "${state.route.size}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.weight(1f))

        MusicControls(
            settingsPrefs = viewModel.settingsPrefs,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(24.dp))

        // Stop button
        ElevatedButton(
            onClick = { showFinishDialog = true },
            modifier = Modifier
                .size(80.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "סיום",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("סיום אימון?") },
            text = {
                Text(
                    "%.2f ק״מ ב-%s".format(
                        state.distanceMeters / 1000.0,
                        formatTime(state.elapsedSecs)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finish()
                }) {
                    Text("סיים", color = Purple)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showFinishDialog = false
                        viewModel.discard(onDiscarded = onFinished)
                    }) {
                        Text("בטל", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text("המשך")
                    }
                }
            }
        )
    }
}

@Composable
private fun CardioSummary(
    state: CardioUiState,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "🎉",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 64.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            if (state.type == "RUNNING") "ריצה הושלמה!" else "הליכה הושלמה!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = SuccessGreen
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("משך", formatTime(state.elapsedSecs), Modifier.weight(1f))
            SummaryCard("מרחק", "%.2f ק״מ".format(state.distanceMeters / 1000.0), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        val avgSpeedKmh = if (state.elapsedSecs > 0) {
            (state.distanceMeters / 1000.0) / (state.elapsedSecs / 3600.0)
        } else 0.0
        val avgPace = if (avgSpeedKmh > 0.5) {
            val paceMin = (60.0 / avgSpeedKmh).toInt()
            val paceSec = ((60.0 / avgSpeedKmh - paceMin) * 60).toInt()
            "%d:%02d /ק״מ".format(paceMin, paceSec)
        } else "—"

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("מהירות ממוצעת", "%.1f קמ״ש".format(avgSpeedKmh), Modifier.weight(1f))
            SummaryCard("קצב ממוצע", avgPace, Modifier.weight(1f))
        }

        if (state.route.size >= 2) {
            Spacer(Modifier.height(24.dp))
            RouteMap(route = state.route)
        }

        if (state.tracks.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))

            Text(
                "♫ פסקול האימון",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            state.tracks.forEachIndexed { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        if (track.artist.isNotBlank()) {
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        ElevatedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.elevatedButtonColors(containerColor = Purple)
        ) {
            Text(
                "סגור",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = Purple,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Purple,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteMap(route: List<LocationPoint>) {
    RunMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(16.dp)),
        route = route,
        follow = false
    )
}

private fun formatTime(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
