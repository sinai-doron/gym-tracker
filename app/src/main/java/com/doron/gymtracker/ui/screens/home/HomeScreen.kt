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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
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
import com.doron.gymtracker.domain.model.TrainingType
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.model.WeatherCondition
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.ProgramStatus
import com.doron.gymtracker.ui.components.AchievementsStrip
import com.doron.gymtracker.ui.components.Activity
import com.doron.gymtracker.ui.components.BadgesRow
import com.doron.gymtracker.ui.components.TrainingTypeBadge
import com.doron.gymtracker.ui.components.dayColors
import com.doron.gymtracker.ui.components.displayName
import com.doron.gymtracker.ui.components.drawDay
import com.doron.gymtracker.ui.components.label
import com.doron.gymtracker.ui.components.localizedName
import com.doron.gymtracker.ui.components.valueText
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.PurpleLight
import com.doron.gymtracker.ui.theme.SuccessGreen
import com.doron.gymtracker.ui.components.CardioCard
import com.doron.gymtracker.ui.components.HomeOverflowMenu
import com.doron.gymtracker.ui.components.NextWorkoutCard
import com.doron.gymtracker.ui.components.SquareIconButton
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import com.doron.gymtracker.ui.theme.AppType
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
    onAchievementsClick: () -> Unit,
    onProgramsClick: () -> Unit,
    onSetUpTraining: () -> Unit,
    onTimer: (typeId: String) -> Unit,
    onTrainingTypes: () -> Unit,
    onLogSession: () -> Unit,
    onLoaded: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    badgesViewModel: BadgesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    // Lets the launch splash go once home has its data
    LaunchedEffect(state.isLoading) { if (!state.isLoading) onLoaded() }
    val badges by badgesViewModel.badges.collectAsState()
    val records by badgesViewModel.records.collectAsState()
    val achievements by badgesViewModel.achievements.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingCardioType by remember { mutableStateOf<String?>(null) }
    val stepsCard = remember { BringIntoViewRequester() }

    val weatherLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refresh() }
    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refresh() }

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
                    style = AppType.headerTitle,
                    color = AppColors.textPrimary,
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
                Row(horizontalArrangement = Arrangement.spacedBy(AppShapes.headerIconButtonGap)) {
                    SquareIconButton(
                        icon = R.drawable.ic_home_calendar,
                        description = stringResource(R.string.history),
                        size = AppShapes.headerIconButton,
                        radius = AppShapes.headerIconButtonRadius,
                        iconSize = AppShapes.headerIconButtonIcon,
                        background = Color.Transparent,
                        tint = AppColors.textSecondary,
                        onClick = onHistoryClick
                    )
                    HomeOverflowMenu(
                        onLogSession = onLogSession,
                        onTrainingTypes = onTrainingTypes,
                        onPrograms = onProgramsClick,
                        onRoutes = onRoutesClick,
                        onAchievements = onAchievementsClick,
                        onSettings = onSettingsClick
                    )
                }
            }

            // The active programme (tap to switch), its week: A/B variants and the next deload
            state.programStatus?.let {
                ProgramChip(it, onProgramsClick)
                Spacer(Modifier.height(4.dp))
                ProgramStatusLine(it)
            }
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

            // No next workout: no programme yet (skipped on first launch), or one with no workouts
            if (!state.isLoading && state.nextWorkoutPreview == null && openSession == null) {
                val status = state.programStatus
                if (status == null) {
                    SetUpCard(
                        title = stringResource(R.string.home_setup_title),
                        text = stringResource(R.string.home_setup_text),
                        button = stringResource(R.string.home_setup_button),
                        onClick = onSetUpTraining
                    )
                } else {
                    SetUpCard(
                        title = stringResource(R.string.home_no_workouts_title, status.programName ?: stringResource(R.string.program_default_name)),
                        text = stringResource(R.string.home_no_workouts_text),
                        button = stringResource(R.string.home_no_workouts_button),
                        onClick = onProgramsClick
                    )
                }
            }

            state.nextWorkoutPreview?.let { preview ->
                NextWorkoutCard(
                    preview = preview,
                    onSwap = onChooseWorkout,
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

            if (state.runShown || state.walkShown) {
                CardioCard(
                    onRun = { startCardio("RUNNING") }.takeIf { state.runShown },
                    onWalk = { startCardio("WALKING") }.takeIf { state.walkShown }
                )
            }

            if (state.timedTypes.isNotEmpty() || state.openTimer != null) {
                Spacer(Modifier.height(12.dp))
                OtherTrainingCard(
                    types = state.timedTypes,
                    openTimer = state.openTimer,
                    openTimerType = state.openTimerType,
                    onTimer = onTimer
                )
            }

            // Achievements: the distance badges, then the rest (climb, mileage, iron, regular,
            // specials) in a strip, newest first, then the nearest to earn; all of them a tap away.
            if (badges.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .padding(16.dp)
                ) {
                    val earned = badges.count { it.isEarned } + achievements.count { it.isEarned }
                    CardTitle(stringResource(R.string.home_achievements_title), trailing = stringResource(R.string.home_achievements_earned, earned, badges.size + achievements.size))
                    Spacer(Modifier.height(12.dp))
                    BadgesRow(badges)
                    if (achievements.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        AchievementsStrip(achievements)
                    }
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
private fun ProgramChip(status: ProgramStatus, onClick: () -> Unit) {
    val name = status.programName ?: stringResource(R.string.program_default_name)
    Text(
        "$name ▾",
        style = MaterialTheme.typography.labelLarge,
        color = Purple,
        modifier = Modifier
            .background(PurpleLight, RoundedCornerShape(50))
            .clickable(onClickLabel = stringResource(R.string.program_choose, name), onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ProgramStatusLine(status: ProgramStatus) {
    val parts = listOfNotNull(
        stringResource(R.string.program_week, status.weekNumber),
        if (status.hasVariants) stringResource(if (status.isWeekB) R.string.week_b else R.string.week_a) else null,
        when (status.weeksUntilDeload) {
            null -> null
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
                workout.localizedName(),
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
private fun SetUpCard(title: String, text: String, button: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        CardTitle(title, activity = Activity.LIFT)
        Spacer(Modifier.height(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Purple),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(button)
        }
    }
}

/**
 * The training types timed rather than recorded (Pilates, football…), one button each; a timer
 * left running comes first. Shown only when there is one; the menu leads to the types.
 */
@Composable
private fun OtherTrainingCard(
    types: List<TrainingType>,
    openTimer: Session?,
    openTimerType: TrainingType?,
    onTimer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        CardTitle(stringResource(R.string.home_other_title))
        Spacer(Modifier.height(8.dp))
        if (openTimer != null && openTimerType != null) {
            TypeRow(openTimerType, stringResource(R.string.home_timer_running)) { onTimer(openTimerType.id) }
        }
        types.filter { it.id != openTimerType?.id }.forEach { type ->
            TypeRow(type, stringResource(R.string.recorder_timer)) { onTimer(type.id) }
        }
    }
}

@Composable
private fun TypeRow(type: TrainingType, detail: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        TrainingTypeBadge(type)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(type.displayName(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Every kind of record, best first in the list's order; ones not set yet show a dash. */
@Composable
private fun PersonalRecords(records: List<PersonalRecord>, onSessionClick: (Long) -> Unit) {
    val byKind = records.associateBy { it.kind }
    val dateFormat = remember { java.text.SimpleDateFormat("d.M.yy", java.util.Locale.getDefault()) }
    Text(
        stringResource(R.string.home_records_title),
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
            Text(kind.label(), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (record != null) {
                Text(
                    if (kind == RecordKind.BEST_WEEK) stringResource(R.string.home_record_week, dateFormat.format(java.util.Date(record.at)))
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
        CardTitle(stringResource(if (stats.rolling) R.string.home_week_rolling else R.string.home_week_calendar), Modifier.padding(horizontal = 8.dp), trailing = weekRange(stats.days))
        Spacer(Modifier.height(12.dp))
        if (stats.days.isNotEmpty()) {
            WeekStrip(stats.days)
            Spacer(Modifier.height(14.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            WeeklyStatItem(stringResource(R.string.workouts), "${stats.sessionsCount}")
            WeeklyStatItem(stringResource(R.string.home_stat_minutes), "${stats.totalMinutes}")
            if (stats.totalVolumeKg > 0) {
                WeeklyStatItem(stringResource(R.string.home_stat_volume), "%,.0f".format(stats.totalVolumeKg))
            }
            WeeklyStatItem(stringResource(R.string.home_stat_streak), "${stats.streakDays}", highlight = stats.streakDays >= 3)
        }
    }
}

/** "25–31 באוקטובר", or across months "29.11–5.12". */
@Composable
private fun weekRange(days: List<WeekDay>): String? {
    val first = days.firstOrNull()?.date ?: return null
    val last = days.last().date
    // Isolated left to right, or Hebrew text around it turns "25–31" into "31–25"
    val range = if (first.month == last.month) "${first.dayOfMonth}–${last.dayOfMonth}"
    else "${first.dayOfMonth}.${first.monthValue}–${last.dayOfMonth}.${last.monthValue}"
    val isolated = "\u2066$range\u2069"
    return if (first.month == last.month) {
        stringResource(R.string.home_week_range_month, isolated, first.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("he")))
    } else isolated
}

/** Sunday to Saturday, or the last seven days ending today (right to left in Hebrew): trained days filled with the workout, today outlined. */
@Composable
private fun WeekStrip(days: List<WeekDay>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEach { day ->
            val colors = dayColors(day.types).ifEmpty {
                // Types not loaded: gym and cardio in their usual colours
                listOfNotNull(SuccessGreen.takeIf { day.cardio }, Purple.takeIf { day.gym })
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, java.util.Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) Purple else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                // Today gets an outer ring with a gap, so it stands out filled or empty
                Box(
                    Modifier
                        .size(40.dp)
                        .border(2.dp, if (day.isToday) Purple else Color.Transparent, CircleShape)
                        .padding(4.dp)
                        .clip(CircleShape)
                        // Each training type in its colour, as in history
                        .drawBehind { drawDay(colors, Purple.copy(alpha = 0.08f)) }
                )
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
