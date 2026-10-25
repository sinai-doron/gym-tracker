package com.doron.gymtracker.ui.screens.session

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.usecase.GetLastWeightUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.ProgressionHintUseCase
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.media.MediaPermissionHelper
import com.doron.gymtracker.media.NowPlayingProvider
import com.doron.gymtracker.ui.components.ExerciseCard
import com.doron.gymtracker.ui.components.NowPlayingBar
import com.doron.gymtracker.ui.theme.RestTimerColor
import com.doron.gymtracker.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    logSetUseCase: LogSetUseCase,
    progressionHintUseCase: ProgressionHintUseCase? = null,
    getLastWeightUseCase: GetLastWeightUseCase? = null,
    settingsPrefs: SettingsPrefs? = null,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailViewModels = remember { mutableStateMapOf<String, ExerciseDetailViewModel>() }

    val detailStates = state.workout?.exercises?.associate { exercise ->
        exercise.id to (detailViewModels.getOrPut(exercise.id) {
            ExerciseDetailViewModel(viewModel.sessionId, exercise, logSetUseCase,
                progressionHint = progressionHintUseCase,
                getLastWeight = getLastWeightUseCase)
        }.uiState.collectAsState().value)
    } ?: emptyMap()

    val activeRestSecs = detailStates.values.mapNotNull { it.restCountdownSecs }.firstOrNull()

    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Sticky header: 3-column Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RTL start: workout name + session clock
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.workout_label, state.workout?.label?.name ?: ""),
                    style = MaterialTheme.typography.titleMedium
                )
                val mins = state.elapsedSecs / 60
                val secs = state.elapsedSecs % 60
                Text(
                    "%02d:%02d".format(mins, secs),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Center: rest timer (hidden when inactive)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (activeRestSecs != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "מנוחה",
                            style = MaterialTheme.typography.labelSmall,
                            color = RestTimerColor
                        )
                        Text(
                            "%02d:%02d".format(activeRestSecs / 60, activeRestSecs % 60),
                            style = MaterialTheme.typography.titleLarge,
                            color = RestTimerColor
                        )
                    }
                }
            }

            // RTL end: compact end-session button
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                FilledTonalButton(onClick = { viewModel.finishSession() }) {
                    Text("סיים")
                }
            }
        }

        // Deload week banner
        if (state.isDeloadWeek) {
            Text(
                stringResource(R.string.deload_week_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.ui.graphics.Color(0xFF1A1035),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningAmber)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Scrollable exercise list
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(state.workout?.exercises ?: emptyList()) { exercise ->
                val detailState = detailStates[exercise.id]
                ExerciseCard(
                    exercise = exercise,
                    completedSets = detailState?.completedSets ?: emptyList(),
                    onClick = { selectedExercise = exercise }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Now Playing bar / permission prompt
        val nowPlaying by NowPlayingProvider.state.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val permDismissed by (settingsPrefs?.mediaPermissionDismissed
            ?: kotlinx.coroutines.flow.flowOf(false)).collectAsState(initial = false)
        var hasPermission by remember { mutableStateOf(MediaPermissionHelper.isNotificationListenerEnabled(context)) }

        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            hasPermission = MediaPermissionHelper.isNotificationListenerEnabled(context)
        }

        if (nowPlaying != null) {
            NowPlayingBar(state = nowPlaying!!)
        } else if (!hasPermission && !permDismissed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "הפעל גישה למדיה כדי לשלוט במוזיקה",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(onClick = {
                    MediaPermissionHelper.openNotificationListenerSettings(context)
                }) {
                    Text("הפעל")
                }
                androidx.compose.material3.IconButton(onClick = {
                    scope.launch { settingsPrefs?.dismissMediaPermission() }
                }) {
                    Text("✕", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    val exercise = selectedExercise
    if (exercise != null) {
        val detailVm = detailViewModels.getOrPut(exercise.id) {
            ExerciseDetailViewModel(viewModel.sessionId, exercise, logSetUseCase,
                progressionHint = progressionHintUseCase,
                getLastWeight = getLastWeightUseCase)
        }
        ModalBottomSheet(onDismissRequest = { selectedExercise = null }, sheetState = sheetState) {
            ExerciseDetailSheet(viewModel = detailVm)
        }
    }
}
