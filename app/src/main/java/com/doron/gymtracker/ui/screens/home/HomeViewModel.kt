package com.doron.gymtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.StepCounterManager
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyStats(
    val sessionsCount: Int = 0,
    val totalMinutes: Int = 0,
    val totalVolumeKg: Float = 0f,
    val streakDays: Int = 0
)

data class HomeUiState(
    val suggestedWorkout: Workout? = null,
    val selectedWorkout: Workout? = null,
    val allWorkouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true,
    val todaySteps: Int = 0,
    val stepsAvailable: Boolean = false,
    val weeklyStats: WeeklyStats = WeeklyStats()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNextWorkout: GetNextWorkoutUseCase,
    private val startSession: StartSessionUseCase,
    private val workoutRepo: WorkoutRepository,
    private val sessionRepo: SessionRepository,
    val stepCounter: StepCounterManager,
    private val healthConnect: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val suggested = getNextWorkout()
            val all = workoutRepo.getAll()
            val hcHasPerms = healthConnect.isAvailable && healthConnect.hasPermissions()
            val hasSteps = stepCounter.isAvailable || hcHasPerms
            _uiState.update {
                it.copy(
                    suggestedWorkout = suggested,
                    selectedWorkout = suggested,
                    allWorkouts = all,
                    isLoading = false,
                    stepsAvailable = hasSteps
                )
            }
            if (hcHasPerms) {
                val hcSteps = healthConnect.readTodaySteps()
                _uiState.update { it.copy(todaySteps = hcSteps.toInt()) }
            }
            loadWeeklyStats()
            if (stepCounter.isAvailable) {
                stepCounter.start()
            }
        }
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
        val weekStart = today.with(DayOfWeek.SUNDAY)
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
        }.toSet().sortedDescending()

        var streak = 0
        var checkDate = today
        for (date in datesWithSessions) {
            if (date == checkDate || date == checkDate.minusDays(1)) {
                if (date == checkDate.minusDays(1)) checkDate = date
                streak++
                checkDate = date.minusDays(1)
            } else if (date.isBefore(checkDate)) {
                break
            }
        }

        _uiState.update {
            it.copy(weeklyStats = WeeklyStats(
                sessionsCount = sessions.size,
                totalMinutes = totalMinutes,
                totalVolumeKg = volume,
                streakDays = streak
            ))
        }
    }
}
