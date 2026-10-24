package com.doron.gymtracker.ui.screens.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.ui.components.ExerciseCard
import com.doron.gymtracker.ui.components.SessionClockDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    viewModel: SessionViewModel,
    onFinished: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var selectedExercise by remember { mutableStateOf<ExerciseDefinition?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onFinished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Workout ${state.workout?.label?.name ?: ""}",
                            style = MaterialTheme.typography.titleLarge
                        )
                        SessionClockDisplay(state.elapsedSecs)
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.workout?.exercises ?: emptyList()) { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        entry = state.entries[exercise.id],
                        onClick = { selectedExercise = exercise }
                    )
                }
                item {
                    Spacer(Modifier.height(80.dp))
                }
            }

            Button(
                onClick = { viewModel.finishSession() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Finish Session")
            }
        }

        if (selectedExercise != null) {
            ModalBottomSheet(
                onDismissRequest = { selectedExercise = null },
                sheetState = sheetState
            ) {
                Text(
                    "Exercise: ${selectedExercise?.name}",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Set logging coming in Task 8",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
