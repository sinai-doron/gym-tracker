package com.doron.gymtracker.ui.screens.session

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.launch
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.usecase.GetLastWeightUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.ProgressionHintUseCase
import com.doron.gymtracker.domain.usecase.UpdateSetUseCase
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.media.MediaPermissionHelper
import com.doron.gymtracker.media.NowPlayingProvider
import com.doron.gymtracker.service.SessionTimerService
import com.doron.gymtracker.ui.components.ExerciseCard
import com.doron.gymtracker.ui.components.NowPlayingBar
import com.doron.gymtracker.ui.theme.RestTimerColor
import com.doron.gymtracker.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    logSetUseCase: LogSetUseCase,
    updateSetUseCase: UpdateSetUseCase? = null,
    progressionHintUseCase: ProgressionHintUseCase? = null,
    getLastWeightUseCase: GetLastWeightUseCase? = null,
    settingsPrefs: SettingsPrefs? = null,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailViewModels = remember { mutableStateMapOf<String, ExerciseDetailViewModel>() }

    fun detailViewModelFor(exercise: ExerciseDefinition) = detailViewModels.getOrPut(exercise.id) {
        ExerciseDetailViewModel(viewModel.sessionId, exercise, logSetUseCase,
            updateSet = updateSetUseCase,
            progressionHint = progressionHintUseCase,
            getLastWeight = getLastWeightUseCase,
            restTimer = viewModel.restTimer,
            onTargetSetsChanged = viewModel::updateTargetSets)
    }

    val detailStates = state.workout?.exercises?.associate { exercise ->
        exercise.id to detailViewModelFor(exercise).uiState.collectAsState().value
    } ?: emptyMap()

    val activeRestSecs = viewModel.restTimer.state.collectAsState().value?.remainingSecs
    val context = LocalContext.current

    // Android 13+ hides the session notification until the user allows notifications
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Start foreground service when workout loads
    LaunchedEffect(state.workout) {
        state.workout?.let {
            SessionTimerService.start(context, it.label.displayName)
        }
    }

    // Sync rest timer to service notification
    LaunchedEffect(activeRestSecs) {
        SessionTimerService.updateRest(activeRestSecs)
    }

    // Show summary screen when finished
    if (state.isFinished) {
        SessionTimerService.stop(context)
        val summaryData = remember {
            buildSummaryData(
                workoutName = state.workout?.label?.displayName ?: "",
                durationSecs = state.elapsedSecs,
                detailStates = detailStates
            )
        }
        SessionSummaryScreen(data = summaryData, onGoHome = onFinished)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Header: workout name + end button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.workout?.label?.displayName ?: "",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = { viewModel.finishSession() }) {
                Text("סיים")
            }
        }

        // Timers side by side — in RTL the first child is on the right:
        // overall session time on the right, rest countdown on the left
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "%02d:%02d".format(state.elapsedSecs / 60, state.elapsedSecs % 60),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "זמן אימון",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rest = activeRestSecs ?: 0
                Text(
                    "%d:%02d".format(rest / 60, rest % 60),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = RestTimerColor
                )
                Text(
                    "מנוחה",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                    exercise = detailState?.exercise ?: exercise,
                    completedSets = detailState?.completedSets ?: emptyList(),
                    onClick = { selectedExercise = exercise }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Now Playing bar / permission prompt
        val nowPlaying by NowPlayingProvider.state.collectAsState()
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
        val detailVm = detailViewModelFor(exercise)
        ModalBottomSheet(onDismissRequest = { selectedExercise = null }, sheetState = sheetState) {
            ExerciseDetailSheet(viewModel = detailVm)
        }
    }
}
