package com.doron.gymtracker.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.ui.components.RestTimerOverlay
import com.doron.gymtracker.ui.components.SetLogRow

@Composable
fun ExerciseDetailSheet(viewModel: ExerciseDetailViewModel, onEntryChanged: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .imePadding()
    ) {
        Text(state.exercise.name, style = MaterialTheme.typography.titleLarge)
        if (state.exercise.isPerSide) {
            Text("לכל צד – הזן לכל צד בנפרד",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))

        state.completedSets.forEachIndexed { i, set -> SetLogRow(setNumber = i + 1, record = set) }

        Spacer(Modifier.height(16.dp))

        if (state.restCountdownSecs != null) {
            RestTimerOverlay(
                countdownSecs = state.restCountdownSecs!!,
                onDismiss = { viewModel.dismissRestTimer() }
            )
        } else {
            when (state.exercise) {
                is RepExerciseDefinition -> RepSetInput(
                    targetReps = (state.exercise as RepExerciseDefinition).targetReps,
                    onLog = { reps, weight -> viewModel.logRepSet(reps, weight); onEntryChanged() }
                )
                is TimedExerciseDefinition -> TimedSetInput(
                    isRunning = state.timerRunning,
                    elapsedSecs = state.timerElapsedSecs,
                    onStart = { viewModel.startTimedSet() },
                    onStop = { viewModel.stopTimedSet(); onEntryChanged() }
                )
                is DistanceExerciseDefinition -> DistanceSetInput(
                    targetDistanceM = (state.exercise as DistanceExerciseDefinition).targetDistanceM,
                    onLog = { distanceM -> viewModel.logDistanceSet(distanceM); onEntryChanged() }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RepSetInput(targetReps: Int, onLog: (Int, Float) -> Unit) {
    var repsText by remember { mutableStateOf(targetReps.toString()) }
    var weightText by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it },
            label = { Text(stringResource(R.string.reps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it },
            label = { Text(stringResource(R.string.weight_kg)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val reps = repsText.toIntOrNull() ?: return@Button
            val weight = weightText.toFloatOrNull() ?: return@Button
            onLog(reps, weight)
            repsText = targetReps.toString()
            weightText = ""
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = repsText.isNotBlank() && weightText.isNotBlank()
    ) {
        Text(stringResource(R.string.log_set))
    }
}

@Composable
private fun TimedSetInput(isRunning: Boolean, elapsedSecs: Int, onStart: () -> Unit, onStop: () -> Unit) {
    if (isRunning) {
        Text(
            "%02d:%02d".format(elapsedSecs / 60, elapsedSecs % 60),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Button(
        onClick = if (isRunning) onStop else onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(if (isRunning) R.string.stop_timer else R.string.start_timer))
    }
}

@Composable
private fun DistanceSetInput(targetDistanceM: Int, onLog: (Int) -> Unit) {
    var distanceText by remember { mutableStateOf("") }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("מרחק (מ׳) — יעד: $targetDistanceM מ׳") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = {
            val dist = distanceText.toIntOrNull() ?: return@Button
            onLog(dist)
            distanceText = ""
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = distanceText.isNotBlank()
    ) {
        Text(stringResource(R.string.log_set))
    }
}
