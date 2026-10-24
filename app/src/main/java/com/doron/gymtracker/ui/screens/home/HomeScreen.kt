package com.doron.gymtracker.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onSessionStart: (sessionId: Long, workoutId: String) -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Gym Tracker", style = MaterialTheme.typography.headlineLarge)

        Spacer(Modifier.height(32.dp))

        Text("Select workout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.allWorkouts.forEach { workout ->
                val isSelected = state.selectedWorkout?.id == workout.id
                val isSuggested = state.suggestedWorkout?.id == workout.id
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectWorkout(workout) },
                    label = {
                        Text(if (isSuggested) "${workout.label.name} ★" else workout.label.name)
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    val result = viewModel.startSession()
                    if (result != null) onSessionStart(result.first, result.second)
                }
            },
            enabled = state.selectedWorkout != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Session")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onHistoryClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("History")
        }
    }
}
