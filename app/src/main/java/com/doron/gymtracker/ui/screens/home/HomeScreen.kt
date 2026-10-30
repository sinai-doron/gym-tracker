package com.doron.gymtracker.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.R
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.model.RecordKind
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.model.WeatherCondition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.domain.usecase.ProgramStatus
import com.doron.gymtracker.ui.components.Activity
import com.doron.gymtracker.ui.components.ActivityIcon
import com.doron.gymtracker.ui.components.BadgesRow
import com.doron.gymtracker.ui.components.ExercisePreviewList
import com.doron.gymtracker.ui.components.WorkoutMetaRow
import com.doron.gymtracker.ui.components.valueText
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSessionStart: (sessionId: Long, workoutId: String) -> Unit,
    onCardioStart: (type: String) -> Unit,
    onChooseWorkout: () -> Unit,
    onExerciseProgress: (exerciseId: String) -> Unit,
    onHistoryClick: () -> Unit,
    onRoutesClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    badgesViewModel: BadgesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val badges by badgesViewModel.badges.collectAsState()
    val records by badgesViewModel.records.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingCardioType by remember { mutableStateOf<String?>(null) }
    val stepsCard = remember { BringIntoViewRequester() }

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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
        badgesViewModel.refresh()
    }

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
                    // Opens the week's steps, further down
                    TextButton(
                        onClick = { scope.launch { stepsCard.bringIntoView() } },
                        enabled = state.stepsWeek != null
                    ) {
                        Text(
                            "👣 %,d".format(state.todaySteps),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Purple
                        )
                    }
                }
                IconButton(onClick = onHistoryClick) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = stringResource(R.string.history),
                        tint = Purple.copy(alpha = 0.6f)
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

            // Weekly stats
            WeeklyStatsRow(state.weeklyStats)

            state.stepsWeek?.let { week ->
                Spacer(Modifier.height(12.dp))
                StepsWeekCard(week, Modifier.bringIntoViewRequester(stepsCard))
            }

            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(12.dp))

            CardioCard(
                routesCount = state.routesCount,
                onRun = { startCardio("RUNNING") },
                onWalk = { startCardio("WALKING") },
                onRoutes = onRoutesClick
            )

            // Achievements: the distance badges for now, more kinds later
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    CardTitle("🏅 הישגים", trailing = "${badges.count { it.isEarned }} מתוך ${badges.size}")
                    Spacer(Modifier.height(12.dp))
                    BadgesRow(badges)
                    if (records.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        PersonalRecords(records, onSessionClick)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
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

private val WEATHER_ROW_HEIGHT = 56.dp

@Composable
private fun NextWorkoutCard(
    preview: WorkoutPreview,
    onShowAll: () -> Unit,
    onExerciseClick: (String) -> Unit,
    onStart: () -> Unit
) {
    // The exercise list folds away, so the gym card doesn't crowd out the rest of the screen.
    // Plain remember: it's folded again whenever home is opened or the next workout changes
    var showExercises by remember(preview.workout.id) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        CardTitle(stringResource(R.string.next_workout), activity = Activity.LIFT)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .background(Purple, CircleShape)
            ) {
                Text(
                    preview.workout.label.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                preview.workout.label.displayName,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }

        Spacer(Modifier.height(10.dp))
        WorkoutMetaRow(preview)
        TextButton(onClick = { showExercises = !showExercises }, contentPadding = PaddingValues(0.dp)) {
            Text(
                if (showExercises) "הסתר תרגילים" else "הצג תרגילים (${preview.exercises.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = Purple
            )
            Icon(
                if (showExercises) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Purple
            )
        }
        if (showExercises) {
            ExercisePreviewList(preview.exercises, onExerciseClick = onExerciseClick)
            Spacer(Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text(
                    stringResource(R.string.start_session),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            // The workouts screen, to pick a different one
            OutlinedButton(
                onClick = onShowAll,
                modifier = Modifier.height(52.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("החלף", style = MaterialTheme.typography.titleSmall, color = Purple)
            }
        }
    }
}

/** Running and walking, in the same green as cardio days in the history calendar, with the way to routes. */
@Composable
private fun CardioCard(
    routesCount: Int,
    onRun: () -> Unit,
    onWalk: () -> Unit,
    onRoutes: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        CardTitle("ריצה והליכה")
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CardioButton(Activity.RUN, "ריצה", Modifier.weight(1f), onRun)
            CardioButton(Activity.WALK, "הליכה", Modifier.weight(1f), onWalk)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onRoutes)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityIcon(Activity.ROUTES, size = 28.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                "מסלולים",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                " · $routesCount שמורים",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** A soft green button with the activity's tile, so it stands beside the gym's start button without outshouting it. */
@Composable
private fun CardioButton(activity: Activity, label: String, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = MaterialTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.12f), contentColor = CardioInk)
    ) {
        ActivityIcon(activity, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
    }
}

/** SuccessGreen darkened enough to read as text on its own light tint. */
private val CardioInk = Color(0xFF007A62)

/** Every kind of record, best first in the list's order; ones not set yet show a dash. */
@Composable
private fun PersonalRecords(records: List<PersonalRecord>, onSessionClick: (Long) -> Unit) {
    val byKind = records.associateBy { it.kind }
    val dateFormat = remember { java.text.SimpleDateFormat("d.M.yy", java.util.Locale.getDefault()) }
    Text(
        "🏆 שיאים אישיים",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(4.dp))
    RecordKind.entries.forEach { kind ->
        val record = byKind[kind]
        val sessionId = record?.sessionId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .then(if (sessionId != null) Modifier.clickable { onSessionClick(sessionId) } else Modifier)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(kind.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (record != null) {
                Text(
                    if (kind == RecordKind.BEST_WEEK) "שבוע " + dateFormat.format(java.util.Date(record.at))
                    else dateFormat.format(java.util.Date(record.at)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                record?.valueText() ?: "—",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = if (record != null) Purple else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyStatsRow(stats: WeeklyStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp)
    ) {
        CardTitle("📅 השבוע", Modifier.padding(horizontal = 8.dp), trailing = weekRange(stats.days))
        Spacer(Modifier.height(12.dp))
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

/** "25–31 באוקטובר", or across months "29.11–5.12". */
private fun weekRange(days: List<WeekDay>): String? {
    val first = days.firstOrNull()?.date ?: return null
    val last = days.last().date
    // Isolated left to right, or Hebrew text around it turns "25–31" into "31–25"
    val range = if (first.month == last.month) "${first.dayOfMonth}–${last.dayOfMonth}"
    else "${first.dayOfMonth}.${first.monthValue}–${last.dayOfMonth}.${last.monthValue}"
    val month = if (first.month == last.month) {
        " ב" + first.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("he"))
    } else ""
    return "\u2066$range\u2069$month"
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
                        .clip(CircleShape)
                        .drawBehind {
                            when {
                                // Both on one day: gym on the right half, cardio on the left, as in history
                                day.gym && day.cardio -> {
                                    drawRect(SuccessGreen, size = Size(size.width / 2, size.height))
                                    drawRect(Purple, topLeft = Offset(size.width / 2, 0f), size = Size(size.width / 2, size.height))
                                }
                                day.gym -> drawRect(Purple)
                                day.cardio -> drawRect(SuccessGreen)
                                else -> drawRect(Purple.copy(alpha = 0.08f))
                            }
                        }
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
