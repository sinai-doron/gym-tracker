package com.doron.gymtracker.ui.screens.session

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.ui.components.RestTimerOverlay
import com.doron.gymtracker.ui.components.SetLogRow

@Composable
fun ExerciseDetailSheet(
    viewModel: ExerciseDetailViewModel,
    onEntryChanged: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(state.exercise.name, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))

        // Completed sets
        state.completedSets.forEachIndexed { i, set ->
            SetLogRow(setNumber = i + 1, record = set)
        }

        Spacer(Modifier.height(16.dp))

        // Rest timer overlay (shown over input area)
        if (state.restCountdownSecs != null) {
            RestTimerOverlay(
                countdownSecs = state.restCountdownSecs!!,
                onDismiss = { viewModel.dismissRestTimer() }
            )
        } else {
            // Input area — branches on exercise type
            when (state.exercise) {
                is RepExerciseDefinition -> RepSetInput(
                    targetReps = (state.exercise as RepExerciseDefinition).targetReps,
                    onLog = { reps, weight ->
                        viewModel.logRepSet(reps, weight)
                        onEntryChanged()
                    }
                )
                is TimedExerciseDefinition -> TimedSetInput(
                    isRunning = state.timerRunning,
                    elapsedSecs = state.timerElapsedSecs,
                    onStart = { viewModel.startTimedSet() },
                    onStop = {
                        viewModel.stopTimedSet()
                        onEntryChanged()
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun RepSetInput(
    targetReps: Int,
    onLog: (Int, Float) -> Unit
) {
    var repsText by remember { mutableStateOf(targetReps.toString()) }
    var weightText by remember { mutableStateOf("") }

    OutlinedTextField(
        value = repsText,
        onValueChange = { repsText = it },
        label = { Text("Reps") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = weightText,
        onValueChange = { weightText = it },
        label = { Text("Weight (kg)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
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
        Text("Log Set")
    }
}

@Composable
private fun TimedSetInput(
    isRunning: Boolean,
    elapsedSecs: Int,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    if (isRunning) {
        Text(
            "%02d:%02d".format(elapsedSecs / 60, elapsedSecs % 60),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Button(
        onClick = if (isRunning) onStop else onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (isRunning) "Stop Timer" else "Start Timer")
    }
}
