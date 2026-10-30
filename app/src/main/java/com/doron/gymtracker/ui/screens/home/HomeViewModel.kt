package com.doron.gymtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.StepCounterManager
import com.doron.gymtracker.data.WeatherRepository
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.domain.usecase.CloseStaleSessionsUseCase
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.GetProgramStatusUseCase
import com.doron.gymtracker.domain.usecase.GetWorkoutPreviewUseCase
import com.doron.gymtracker.domain.usecase.ProgramStatus
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeeklyStats(
    val sessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val totalVolumeKg: Float = 0f,
    val streakDays: Int = 0,
    /** Sunday to Saturday of the current week. */
    val days: List<WeekDay> = emptyList()
)

/** One day in the week strip. [mark] is what was trained (workout letter or a cardio icon), or null. */
data class WeekDay(val date: LocalDate, val mark: String?, val isToday: Boolean)

data class HomeUiState(
    val suggestedWorkout: Workout? = null,
    val selectedWorkout: Workout? = null,
    val nextWorkoutPreview: WorkoutPreview? = null,
    val allWorkouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val todaySteps: Int = 0,
    val stepsAvailable: Boolean = false,
    val healthConnectNeedsPermission: Boolean = false,
    val weather: Weather? = null,
    val weatherNeedsPermission: Boolean = false,
    val weatherLoading: Boolean = false,
    /** A gym session left open (e.g. the app was closed mid-workout), offered for resuming. */
    val openSession: Session? = null,
    val openSessionWorkout: Workout? = null,
    val programStatus: ProgramStatus? = null,
    val weeklyStats: WeeklyStats = WeeklyStats(),
    /** This week's steps by day; null when Health Connect's steps can't be read. */
    val stepsWeek: StepsWeek? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNextWorkout: GetNextWorkoutUseCase,
    private val startSession: StartSessionUseCase,
    private val workoutRepo: WorkoutRepository,
    private val sessionRepo: SessionRepository,
    val stepCounter: StepCounterManager,
    private val healthConnect: HealthConnectManager,
    private val getWorkoutPreview: GetWorkoutPreviewUseCase,
    private val closeStaleSessions: CloseStaleSessionsUseCase,
    private val weatherRepo: WeatherRepository,
    private val getProgramStatus: GetProgramStatusUseCase,
    private val settingsPrefs: SettingsPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            stepCounter.todaySteps.collect { steps ->
                val current = _uiState.value.todaySteps
                if (steps > current) _uiState.update { it.copy(todaySteps = steps) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepCounter.stop()
    }

    /** Reloads everything that changes after a workout: next workout, its preview, stats. */
    fun refresh() {
        // Weather is fetched separately so a slow network never delays the rest of the screen
        viewModelScope.launch {
            val needsPermission = !weatherRepo.hasLocationPermission()
            _uiState.update { it.copy(weatherNeedsPermission = needsPermission, weatherLoading = !needsPermission) }
            if (!needsPermission) {
                val weather = weatherRepo.currentWeather()
                _uiState.update { it.copy(weather = weather ?: it.weather, weatherLoading = false) }
            }
        }
        viewModelScope.launch {
            closeStaleSessions()
            val suggested = getNextWorkout()
            val all = workoutRepo.getAll()
            val openSession = sessionRepo.getOpenGymSession()
            val preview = getWorkoutPreview(suggested)
            val hcHasPerms = healthConnect.isAvailable && healthConnect.hasPermissions()
            // Permission may have just been granted, so try starting the sensor on every refresh
            stepCounter.start()
            val hasSteps = stepCounter.isAvailable || hcHasPerms
            _uiState.update {
                it.copy(
                    suggestedWorkout = suggested,
                    selectedWorkout = suggested,
                    nextWorkoutPreview = preview,
                    openSession = openSession,
                    openSessionWorkout = all.firstOrNull { w -> w.id == openSession?.workoutId },
                    programStatus = getProgramStatus(),
                    allWorkouts = all,
                    isLoading = false,
                    stepsAvailable = hasSteps,
                    healthConnectNeedsPermission = healthConnect.isAvailable && !hcHasPerms
                )
            }
            if (hcHasPerms) {
                val hcSteps = healthConnect.readTodaySteps()
                _uiState.update { it.copy(todaySteps = hcSteps.toInt()) }
            }
            loadWeeklyStats()
        }
    }

    fun selectWorkout(workout: Workout) {
        _uiState.update { it.copy(selectedWorkout = workout) }
    }

    suspend fun startSession(): Pair<Long, String>? {
        val workout = _uiState.value.selectedWorkout ?: return null
        val sessionId = startSession(workout.id)
        return Pair(sessionId, workout.id)
    }

    suspend fun startCardioSession(type: String): Long {
        return sessionRepo.startCardioSession(type)
    }

    private suspend fun loadWeeklyStats() {
        val today = LocalDate.now()
        // previousOrSame: with(SUNDAY) uses Monday-based ISO weeks and jumps to the coming Sunday
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val weekStartMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val sessions = sessionRepo.getCompletedSince(weekStartMillis)
        val totalMinutes = sessions.sumOf { s ->
            val fin = s.finishedAt ?: return@sumOf 0L
            (fin - s.startedAt) / 60_000
        }.toInt()
        val volume = sessionRepo.getTotalVolumeSince(weekStartMillis)

        val allSessions = sessionRepo.getCompletedSince(0)
        val datesWithSessions = allSessions.mapNotNull { s ->
            s.finishedAt?.let {
                java.time.Instant.ofEpochMilli(s.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }.toSet()
        val streak = streakDays(datesWithSessions, today)
        val labels = _uiState.value.allWorkouts.associate { it.id to it.label.name }
        val days = weekDays(weekStart, today, sessions, labels, ZoneId.systemDefault())

        _uiState.update {
            it.copy(weeklyStats = WeeklyStats(
                sessionsCount = sessions.size,
                totalMinutes = totalMinutes,
                totalVolumeKg = volume,
                streakDays = streak,
                days = days
            ))
        }
        loadStepsWeek(weekStart, today, sessions)
    }

    private suspend fun loadStepsWeek(weekStart: LocalDate, today: LocalDate, weekSessions: List<Session>) {
        // Last week too, to compare with
        val daily = healthConnect.readDailySteps(weekStart.minusDays(7), 14) ?: return
        val zone = ZoneId.systemDefault()
        val workout = weekSessions.filter { it.type != "GYM" }
            .mapNotNull { s -> sessionRepo.getRunDetails(s.id)?.steps?.let { s to it.toLong() } }
            .groupBy({ (s, _) -> java.time.Instant.ofEpochMilli(s.startedAt).atZone(zone).toLocalDate() }) { it.second }
            .mapValues { it.value.sum() }
        val week = StepsWeek.build(
            weekStart, today, daily, workout,
            todaySteps = _uiState.value.todaySteps.toLong(),
            goal = settingsPrefs.stepGoal.first()
        )
        _uiState.update { it.copy(stepsWeek = week) }
    }
}

/**
 * Consecutive days with a finished session, ending today — or yesterday, so the
 * streak isn't shown as broken before today's workout.
 */
internal fun streakDays(datesWithSessions: Set<LocalDate>, today: LocalDate): Int {
    var day = if (today in datesWithSessions) today else today.minusDays(1)
    var streak = 0
    while (day in datesWithSessions) {
        streak++
        day = day.minusDays(1)
    }
    return streak
}

/**
 * The seven days from [weekStart], each marked with what was trained that day: the gym workout's
 * letter if there was one, otherwise a run or walk icon.
 */
internal fun weekDays(
    weekStart: LocalDate,
    today: LocalDate,
    sessions: List<Session>,
    workoutLabels: Map<String, String>,
    zone: ZoneId
): List<WeekDay> {
    val byDate = sessions.groupBy { java.time.Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
    return (0L until 7L).map { offset ->
        val date = weekStart.plusDays(offset)
        val daySessions = byDate[date].orEmpty()
        val mark = daySessions.firstOrNull { it.type == "GYM" }?.let { workoutLabels[it.workoutId] }
            ?: daySessions.firstOrNull()?.let { if (it.type == "RUNNING") "🏃" else "🚶" }
        WeekDay(date, mark, isToday = date == today)
    }
}
