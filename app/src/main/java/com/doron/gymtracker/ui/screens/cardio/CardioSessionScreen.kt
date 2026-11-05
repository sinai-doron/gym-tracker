package com.doron.gymtracker.ui.screens.cardio

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.model.RunBadge
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.ui.components.BadgeImage
import com.doron.gymtracker.ui.components.IconLabel
import com.doron.gymtracker.ui.components.MusicControls
import com.doron.gymtracker.ui.components.NewAchievementsDialog
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.components.label
import com.doron.gymtracker.ui.components.valueText
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import kotlin.math.roundToInt
import com.doron.gymtracker.ui.components.distanceText
import com.doron.gymtracker.ui.components.distanceNumber
import com.doron.gymtracker.ui.components.distanceUnitLabel
import com.doron.gymtracker.ui.components.paceText
import com.doron.gymtracker.ui.components.speedText
import com.doron.gymtracker.ui.components.heightText
import com.doron.gymtracker.ui.components.targetNumber

@Composable
fun CardioSessionScreen(
    viewModel: CardioSessionViewModel,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showFinishDialog by remember { mutableStateOf(false) }

    if (state.isFinished) {
        RunSummaryScreen(
            state = state,
            title = stringResource(if (state.type == "WALKING") R.string.run_summary_title_walk else R.string.run_summary_title_run),
            onBack = onFinished,
            savedRouteName = state.savedRouteName,
            onSaveAsRoute = viewModel::saveAsRoute
        )
        if (state.newBadges.isNotEmpty()) {
            NewBadgesDialog(state.newBadges, onDismiss = viewModel::badgesSeen)
        } else if (state.newAchievements.isNotEmpty()) {
            NewAchievementsDialog(state.newAchievements, onDismiss = viewModel::achievementsSeen)
        } else if (state.newRecords.isNotEmpty()) {
            NewRecordsDialog(state.newRecords, onDismiss = viewModel::recordsSeen)
        }
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
            route = state.route,
            plannedRoute = state.plannedRoute
        )

        Spacer(Modifier.height(12.dp))

        Text(
            formatTime(state.elapsedSecs),
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = if (state.isPaused) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else Purple
        )
        if (state.isPaused || state.isManuallyPaused) {
            Text(
                stringResource(if (state.isManuallyPaused) R.string.cardio_paused_manual else R.string.cardio_paused_auto),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.targetProgress?.let { progress ->
            Spacer(Modifier.height(8.dp))
            TargetProgress(state, progress)
        }
        if (state.isOffRoute) {
            Text(
                stringResource(R.string.cardio_off_route),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                label = stringResource(R.string.cardio_distance),
                value = distanceText(state.distanceMeters),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = stringResource(R.string.cardio_pace),
                value = state.currentPaceSecsPerKm?.let { paceText(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatBox(
                label = stringResource(R.string.cardio_avg_pace),
                value = state.avgPaceSecsPerKm?.let { paceText(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            if (state.lapMeters > 0) {
                StatBox(
                    label = stringResource(R.string.cardio_current_lap_distance, state.currentLapNumber, distanceText(state.currentLapMeters)),
                    value = formatTime(state.currentLapSecs),
                    modifier = Modifier.weight(1f)
                )
            } else {
                StatBox(
                    label = stringResource(R.string.cardio_speed),
                    value = if (state.currentSpeed > 0.3f) speedText(state.currentSpeed * 3.6) else "—",
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

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
            // Pause by hand, e.g. for a long stop; auto-pause covers the short ones
            ElevatedButton(
                onClick = { if (state.isManuallyPaused) viewModel.resume() else viewModel.pause() },
                modifier = Modifier.size(64.dp),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = Purple)
            ) {
                Icon(
                    painterResource(if (state.isManuallyPaused) R.drawable.ic_play else R.drawable.ic_pause),
                    contentDescription = stringResource(if (state.isManuallyPaused) R.string.cardio_resume else R.string.cardio_pause),
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
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
                    contentDescription = stringResource(R.string.cardio_finish),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text(stringResource(R.string.cardio_finish_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.cardio_finish_distance,
                        distanceText(state.distanceMeters),
                        formatTime(state.elapsedSecs)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    viewModel.finish()
                }) {
                    Text(stringResource(R.string.cardio_finish_confirm), color = Purple)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showFinishDialog = false
                        viewModel.discard(onDiscarded = onFinished)
                    }) {
                        Text(stringResource(R.string.cardio_discard), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { showFinishDialog = false }) {
                        Text(stringResource(R.string.cardio_keep_going))
                    }
                }
            }
        )
    }
}

/** Celebrates badges earned for the first time; the biggest one leads. */
@Composable
private fun NewBadgesDialog(badges: List<RunBadge>, onDismiss: () -> Unit) {
    val top = badges.maxBy { it.meters }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "badge"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cardio_badges_ok), color = Purple) } },
        title = {
            Text(
                pluralStringResource(R.plurals.cardio_new_badges, badges.size, badges.size),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    badges.sortedBy { it.meters }.forEach {
                        BadgeImage(
                            it, earned = true,
                            size = if (it == top) 160.dp else 72.dp,
                            modifier = Modifier.scale(scale)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.cardio_first_badge, top.label),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
private fun NewRecordsDialog(records: List<PersonalRecord>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cardio_records_ok), color = Purple) } },
        title = {
            Text(
                pluralStringResource(R.plurals.cardio_new_records, records.size, records.size),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                records.forEach { record ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            record.kind.label(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            record.valueText(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Purple
                        )
                    }
                }
            }
        }
    )
}

/** Steps and cadence, climb, calories and weather, as far as they're known. */
@Composable
private fun RunExtras(state: CardioUiState) {
    val items = listOfNotNull(
        state.steps?.let { steps ->
            R.drawable.ic_activity_walk to (state.cadence?.let { stringResource(R.string.cardio_steps_cadence_extra, steps, it) } ?: "%,d".format(steps))
        },
        R.drawable.ic_climb to heightText(state.elevationGainM),
        state.calories?.let { R.drawable.ic_flame to stringResource(R.string.cardio_kcal_extra, it) },
        state.weather?.let { R.drawable.ic_cloud to "${it.temperatureC}°" }
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEach { (icon, text) ->
            IconLabel(
                icon,
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                iconSize = 16.dp,
                maxLines = 1
            )
        }
    }
}

/** "2.30 / 5 km" (or miles) or "12:40 / 30:00" with a bar; green once the target is reached. */
@Composable
private fun TargetProgress(state: CardioUiState, progress: Float) {
    val reached = progress >= 1f
    val color = if (reached) SuccessGreen else Purple
    val text = when (state.target.type) {
        RunTargetType.ROUTE -> stringResource(R.string.cardio_progress_of, distanceNumber(state.routeAlongM), distanceNumber(state.routeLengthM, 1), distanceUnitLabel())
        RunTargetType.DISTANCE -> stringResource(R.string.cardio_progress_of, distanceNumber(state.distanceMeters), targetNumber(state.target), distanceUnitLabel())
        else -> "${formatTime(state.elapsedSecs)} / ${formatTime(state.target.durationMin * 60L)}"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconLabel(
                when {
                    reached -> R.drawable.ic_check
                    state.routeLengthM > 0 -> R.drawable.ic_route
                    else -> R.drawable.ic_target
                },
                stringResource(
                    when {
                        reached && state.routeLengthM > 0 -> R.string.cardio_route_finished
                        reached -> R.string.cardio_target_reached
                        state.routeLengthM > 0 -> R.string.cardio_route_heading
                        else -> R.string.cardio_target_heading
                    }
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = color,
                iconSize = 16.dp,
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

internal fun formatTime(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
