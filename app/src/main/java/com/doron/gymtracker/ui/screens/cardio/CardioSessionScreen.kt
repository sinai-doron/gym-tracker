package com.doron.gymtracker.ui.screens.cardio

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.Lap
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.ui.components.ElevationChart
import com.doron.gymtracker.ui.components.MusicControls
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import kotlin.math.roundToInt

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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The route so far, following the runner
        RunMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp)),
            position = state.route.lastOrNull(),
            route = state.route
        )

        Spacer(Modifier.height(12.dp))

        Text(
            formatTime(state.elapsedSecs),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = Purple
        )

        state.targetProgress?.let { progress ->
            Spacer(Modifier.height(8.dp))
            TargetProgress(state, progress)
        }

        Spacer(Modifier.height(12.dp))

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
                label = "קצב ממוצע",
                value = state.avgPaceSecsPerKm?.let { "${formatPace(it)} /ק״מ" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            if (state.lapMeters > 0) {
                StatBox(
                    label = "הקפה ${state.currentLapNumber} · %.2f ק״מ".format(state.currentLapMeters / 1000.0),
                    value = formatTime(state.currentLapSecs),
                    modifier = Modifier.weight(1f)
                )
            } else {
                StatBox(
                    label = "מהירות",
                    value = if (state.currentSpeed > 0.3f) "%.1f קמ״ש".format(state.currentSpeed * 3.6f) else "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        RunExtras(state)

        Spacer(Modifier.height(12.dp))

        MusicControls(
            settingsPrefs = viewModel.settingsPrefs,
            modifier = Modifier.clip(RoundedCornerShape(16.dp))
        )

        Spacer(Modifier.height(12.dp))

        // Stop button
        ElevatedButton(
            onClick = { showFinishDialog = true },
            modifier = Modifier.size(64.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "סיום",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(30.dp)
            )
        }
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

        state.weather?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                "${it.condition.emoji} ${it.temperatureC}° בתחילת ה${if (state.type == "RUNNING") "ריצה" else "הליכה"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("צעדים", state.steps?.let { "%,d".format(it) } ?: "—", Modifier.weight(1f))
            SummaryCard("קדנס ממוצע", state.avgCadence?.let { "$it צעדים/ד׳" } ?: "—", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard("עלייה מצטברת", "${state.elevationGainM.roundToInt()} מ׳", Modifier.weight(1f))
            SummaryCard("קלוריות", state.calories?.let { "$it קק״ל" } ?: "הגדר משקל", Modifier.weight(1f))
        }

        if (state.route.size >= 2) {
            Spacer(Modifier.height(24.dp))
            RouteMap(route = state.route)
        }

        val profile = remember(state.route) { Elevation.profile(state.route) }
        if (profile.size >= 2) {
            Spacer(Modifier.height(24.dp))
            Text(
                "גובה",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            ElevationChart(
                points = profile,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            )
        }

        if (state.laps.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            LapTable(state.laps)
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

/** Steps and cadence, climb, calories and weather, as far as they're known. */
@Composable
private fun RunExtras(state: CardioUiState) {
    val items = listOfNotNull(
        state.steps?.let { steps -> "👟 %,d".format(steps) + (state.cadence?.let { " · $it/ד׳" } ?: "") },
        "⛰ ${state.elevationGainM.roundToInt()} מ׳",
        state.calories?.let { "🔥 $it קק״ל" },
        state.weather?.let { "${it.condition.emoji} ${it.temperatureC}°" }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/** "2.30 / 5 ק״מ" or "12:40 / 30:00" with a bar; green once the target is reached. */
@Composable
private fun TargetProgress(state: CardioUiState, progress: Float) {
    val reached = progress >= 1f
    val color = if (reached) SuccessGreen else Purple
    val text = when (state.target.type) {
        RunTargetType.DISTANCE -> "%.2f / %s ק״מ".format(state.distanceMeters / 1000.0, formatKm(state.target.distanceKm))
        else -> "${formatTime(state.elapsedSecs)} / ${formatTime(state.target.durationMin * 60L)}"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (reached) "🎯 המטרה הושגה!" else "🎯 מטרה",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                modifier = Modifier.weight(1f)
            )
            Text(
                // LTR isolate keeps "2.30 / 5" in order in RTL
                "\u2066$text\u2069",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress.coerceAtMost(1f) },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
    }
}

/** Each lap's time and pace, the fastest in purple. */
@Composable
private fun LapTable(laps: List<Lap>) {
    val fastest = laps.minByOrNull { it.paceSecsPerKm }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "הקפות",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            laps.forEach { lap ->
                val best = lap == fastest && laps.size > 1
                val weight = if (best) FontWeight.Bold else FontWeight.Normal
                val color = if (best) Purple else MaterialTheme.colorScheme.onSurface
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Text(
                        "${lap.number} · %s ק״מ".format(formatKm((lap.distanceMeters / 1000).toFloat())),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = weight),
                        color = color,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatTime(lap.durationSecs),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = weight),
                        color = color,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(
                        "${formatPace(lap.paceSecsPerKm)} /ק״מ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = weight),
                        color = color,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(vertical = 12.dp, horizontal = 12.dp),
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

private fun formatPace(secsPerKm: Long): String = "%d:%02d".format(secsPerKm / 60, secsPerKm % 60)

private fun formatKm(km: Float): String =
    if (km == km.toInt().toFloat()) km.toInt().toString() else "%.1f".format(km)

private fun formatTime(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
