package com.doron.gymtracker.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.runtime.LaunchedEffect
import androidx.health.connect.client.PermissionController
import com.doron.gymtracker.data.HealthConnectManager
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.ProgramStatus
import com.doron.gymtracker.domain.model.WeatherCondition
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.ui.components.ExercisePreviewList
import com.doron.gymtracker.ui.components.WorkoutMetaRow
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onSessionStart: (sessionId: Long, workoutId: String) -> Unit,
    onCardioStart: (type: String) -> Unit,
    onChooseWorkout: () -> Unit,
    onExerciseProgress: (exerciseId: String) -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingCardioType by remember { mutableStateOf<String?>(null) }

    // Step counting needs ACTIVITY_RECOGNITION (sensor) or Health Connect (full-day steps)
    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }
    val weatherLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    // Coming back from a workout changes the next workout and the stats
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) {
            pendingCardioType?.let { type -> onCardioStart(type) }
        }
        pendingCardioType = null
    }

    fun startCardio(type: String) {
        val hasLoc = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLoc) {
            onCardioStart(type)
        } else {
            pendingCardioType = type
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
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
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            // Header: app name + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (state.healthConnectNeedsPermission) {
                    TextButton(onClick = { healthConnectPermissionLauncher.launch(HealthConnectManager.PERMISSIONS) }) {
                        Text(
                            "👣 " + stringResource(R.string.connect_steps),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Purple
                        )
                    }
                } else if (state.stepsAvailable) {
                    Text(
                        "👣 %,d".format(state.todaySteps),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Purple
                    )
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = Purple.copy(alpha = 0.6f)
                    )
                }
            }

            // Programme week: A/B variants and the next deload
            state.programStatus?.let { ProgramStatusLine(it) }
            Spacer(Modifier.height(8.dp))

            // Weather
            val weather = state.weather
            if (weather != null) {
                WeatherRow(weather)
            } else if (state.weatherLoading) {
                // Hold the row's space so the cards below don't jump when the weather arrives
                Spacer(Modifier.height(WEATHER_ROW_HEIGHT))
            } else if (state.weatherNeedsPermission) {
                TextButton(
                    onClick = { weatherLocationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "📍 " + stringResource(R.string.show_weather),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Purple
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val openSession = state.openSession
            val openWorkout = state.openSessionWorkout
            if (openSession != null && openWorkout != null) {
                ResumeWorkoutCard(
                    session = openSession,
                    workout = openWorkout,
                    onResume = { onSessionStart(openSession.id, openSession.workoutId) }
                )
                Spacer(Modifier.height(12.dp))
            }

            state.nextWorkoutPreview?.let { preview ->
                NextWorkoutCard(
                    preview = preview,
                    onShowAll = onChooseWorkout,
                    onExerciseClick = onExerciseProgress,
                    onStart = {
                        scope.launch {
                            val result = viewModel.startSession()
                            if (result != null) onSessionStart(result.first, result.second)
                        }
                    }
                )
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = onChooseWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    stringResource(R.string.choose_other_workout),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = Purple
                )
            }

            Spacer(Modifier.height(20.dp))

            // Weekly stats
            WeeklyStatsRow(state.weeklyStats)

            Spacer(Modifier.height(14.dp))

            // Cardio buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CardioButton("🏃 ריצה", Modifier.weight(1f)) { startCardio("RUNNING") }
                CardioButton("🚶 הליכה", Modifier.weight(1f)) { startCardio("WALKING") }
            }

            Spacer(Modifier.height(6.dp))

            // History text button
            TextButton(
                onClick = onHistoryClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    stringResource(R.string.history),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Purple.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProgramStatusLine(status: ProgramStatus) {
    val parts = listOf(
        stringResource(R.string.program_week, status.weekNumber),
        stringResource(if (status.isWeekB) R.string.week_b else R.string.week_a),
        when (status.weeksUntilDeload) {
            0 -> stringResource(R.string.deload_now)
            1 -> stringResource(R.string.deload_next_week)
            else -> stringResource(R.string.deload_in_weeks, status.weeksUntilDeload)
        }
    )
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = if (status.isDeloadWeek) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ResumeWorkoutCard(session: Session, workout: Workout, onResume: () -> Unit) {
    val startTime = remember(session.startedAt) {
        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(session.startedAt))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Purple.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.open_workout),
                style = MaterialTheme.typography.labelMedium,
                color = Purple
            )
            Text(
                workout.label.displayName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                stringResource(R.string.started_at, startTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(
            onClick = onResume,
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(stringResource(R.string.resume_workout))
        }
    }
}

@Composable
private fun WeatherRow(weather: Weather) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(WEATHER_ROW_HEIGHT)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(weather.condition.emoji, fontSize = 28.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            "${weather.temperatureC}°",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(weather.condition.labelRes()),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        // LTR isolate keeps "↑28° ↓19°" in order inside RTL text
        Text(
            "\u2066↑${weather.highC}°  ↓${weather.lowC}°\u2069",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun WeatherCondition.labelRes(): Int = when (this) {
    WeatherCondition.CLEAR -> R.string.weather_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.weather_cloudy
    WeatherCondition.FOG -> R.string.weather_fog
    WeatherCondition.DRIZZLE -> R.string.weather_drizzle
    WeatherCondition.RAIN -> R.string.weather_rain
    WeatherCondition.SNOW -> R.string.weather_snow
    WeatherCondition.THUNDERSTORM -> R.string.weather_thunderstorm
}

private const val HOME_EXERCISES_SHOWN = 3
private val WEATHER_ROW_HEIGHT = 56.dp

@Composable
private fun NextWorkoutCard(
    preview: WorkoutPreview,
    onShowAll: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .background(Purple, CircleShape)
            ) {
                Text(
                    preview.workout.label.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    stringResource(R.string.next_workout),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    preview.workout.label.displayName,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        WorkoutMetaRow(preview)
        Spacer(Modifier.height(8.dp))
        ExercisePreviewList(preview.exercises.take(HOME_EXERCISES_SHOWN), onExerciseClick = onExerciseClick)
        val hidden = preview.exercises.size - HOME_EXERCISES_SHOWN
        if (hidden > 0) {
            // The workouts screen opens on the upcoming workout, so this shows the full list
            TextButton(onClick = onShowAll, contentPadding = PaddingValues(0.dp)) {
                Text(
                    stringResource(R.string.more_exercises, hidden),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Purple
                )
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }

        Button(
            onClick = onStart,
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
    }
}

@Composable
private fun CardioButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Purple
        )
    }
}

@Composable
private fun WeeklyStatsRow(stats: WeeklyStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp)
    ) {
        if (stats.days.isNotEmpty()) {
            WeekStrip(stats.days)
            Spacer(Modifier.height(14.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            WeeklyStatItem("אימונים", "${stats.sessionsCount}")
            WeeklyStatItem("דקות", "${stats.totalMinutes}")
            if (stats.totalVolumeKg > 0) {
                WeeklyStatItem("נפח", "%,.0f".format(stats.totalVolumeKg))
            }
            WeeklyStatItem("רצף", "${stats.streakDays}", highlight = stats.streakDays >= 3)
        }
    }
}

/** Sunday to Saturday (right to left in Hebrew): trained days filled with the workout, today outlined. */
@Composable
private fun WeekStrip(days: List<WeekDay>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) Purple else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // Today gets an outer ring with a gap, so it stands out filled or empty
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .border(2.dp, if (day.isToday) Purple else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .background(
                            if (day.mark != null) Purple else Purple.copy(alpha = 0.08f),
                            CircleShape
                        )
                ) {
                    Text(
                        day.mark ?: "",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyStatItem(label: String, value: String, highlight: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = if (highlight) SuccessGreen else Purple
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
