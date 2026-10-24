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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.ui.components.ExerciseCard
import com.doron.gymtracker.ui.components.SessionClockDisplay
import com.doron.gymtracker.ui.theme.RestTimerColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(viewModel: SessionViewModel, logSetUseCase: LogSetUseCase, onFinished: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var selectedExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailViewModels = remember { mutableStateMapOf<String, ExerciseDetailViewModel>() }

    // Collect all detail states so the session screen can read rest countdown + completed sets
    val detailStates = state.workout?.exercises?.associate { exercise ->
        exercise.id to (detailViewModels.getOrPut(exercise.id) {
            ExerciseDetailViewModel(viewModel.sessionId, exercise, logSetUseCase)
        }.uiState.collectAsState().value)
    } ?: emptyMap()

    // Active rest countdown: find any exercise currently in a rest period
    val activeRestSecs by remember {
        derivedStateOf { detailStates.values.mapNotNull { it.restCountdownSecs }.firstOrNull() }
    }

    LaunchedEffect(state.isFinished) { if (state.isFinished) onFinished() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                stringResource(R.string.workout_label, state.workout?.label?.name ?: ""),
                                style = MaterialTheme.typography.titleLarge
                            )
                            SessionClockDisplay(state.elapsedSecs)
                        }
                        // Rest timer — visible next to main clock whenever rest is active
                        if (activeRestSecs != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("מנוחה", style = MaterialTheme.typography.labelLarge,
                                    color = RestTimerColor)
                                Text(
                                    "%02d:%02d".format(activeRestSecs!! / 60, activeRestSecs!! % 60),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = RestTimerColor
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
            // Deload week banner
            if (state.isDeloadWeek) {
                Text(
                    stringResource(R.string.deload_week_banner),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.workout?.exercises ?: emptyList()) { exercise ->
                    val detailState = detailStates[exercise.id]
                    ExerciseCard(
                        exercise = exercise,
                        completedSets = detailState?.completedSets ?: emptyList(),
                        onClick = { selectedExercise = exercise }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
            } // end Column
            Button(
                onClick = { viewModel.finishSession() },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)
            ) {
                Text(stringResource(R.string.finish_session))
            }
        }

        val exercise = selectedExercise
        if (exercise != null) {
            val detailVm = detailViewModels.getOrPut(exercise.id) {
                ExerciseDetailViewModel(viewModel.sessionId, exercise, logSetUseCase)
            }
            ModalBottomSheet(onDismissRequest = { selectedExercise = null }, sheetState = sheetState) {
                ExerciseDetailSheet(viewModel = detailVm)
            }
        }
    }
}
