package com.doron.gymtracker.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.PurpleLight
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onSessionStart: (sessionId: Long, workoutId: String) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Settings gear — top start (RTL: top-right)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                tint = Purple.copy(alpha = 0.6f)
            )
        }

        if (state.isLoading) {
            CircularProgressIndicator(
                color = Purple,
                modifier = Modifier.align(Alignment.Center)
            )
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.1f))

            // Hero illustration area
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(220.dp)
            ) {
                // Decorative background circles
                Canvas(modifier = Modifier.size(220.dp)) {
                    drawCircle(
                        color = PurpleLight,
                        radius = 110.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Purple.copy(alpha = 0.08f),
                        radius = 80.dp.toPx(),
                        center = Offset(center.x - 20.dp.toPx(), center.y + 15.dp.toPx())
                    )
                }
                Image(
                    painter = painterResource(R.drawable.ic_dumbbell_hero),
                    contentDescription = null,
                    modifier = Modifier.size(140.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // App title
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            // Step counter
            if (state.stepsAvailable) {
                val steps = state.todaySteps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        "👣",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%,d".format(steps),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Purple
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "צעדים היום",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))
            } else {
                // Subtitle when no step sensor
                Text(
                    stringResource(R.string.select_workout),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Workout chips
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                state.allWorkouts.forEach { workout ->
                    val isSelected = state.selectedWorkout?.id == workout.id
                    val isSuggested = state.suggestedWorkout?.id == workout.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectWorkout(workout) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Purple,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        label = {
                            val label = workout.label.name
                            Text(
                                if (isSuggested) "$label ${stringResource(R.string.suggested_marker)}" else label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // Big start button
            Button(
                onClick = {
                    scope.launch {
                        val result = viewModel.startSession()
                        if (result != null) onSessionStart(result.first, result.second)
                    }
                },
                enabled = state.selectedWorkout != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text(
                    stringResource(R.string.start_session),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(12.dp))

            // History text button
            TextButton(onClick = onHistoryClick) {
                Text(
                    stringResource(R.string.history),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Purple.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.weight(0.15f))
        }
    }
}
