package com.doron.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.WorkoutLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ExerciseSummary(val exerciseName: String, val sets: List<SetRecord>)

data class SessionSummaryItem(
    val sessionId: Long,
    val sessionType: String,
    val workoutLabel: WorkoutLabel?,
    val workoutName: String,
    val timeRange: String,
    val durationMinutes: Long,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val distanceKm: Double?,
    val exerciseSummaries: List<ExerciseSummary>
)

data class DaySessionsUiState(
    val dateLabel: String,
    val sessions: List<SessionSummaryItem>
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _sessionDates = MutableStateFlow<Set<LocalDate>>(emptySet())
    val sessionDates: StateFlow<Set<LocalDate>> = _sessionDates.asStateFlow()

    private val _selectedDaySessions = MutableStateFlow<DaySessionsUiState?>(null)
    val selectedDaySessions: StateFlow<DaySessionsUiState?> = _selectedDaySessions.asStateFlow()

    private val heLocale = Locale("he")
    private val timeFormat = SimpleDateFormat("HH:mm", heLocale)
    private val dateFormat = SimpleDateFormat("dd בMMMM yyyy", heLocale)
    private val zone = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            sessionRepo.observeAll().collectLatest { sessions ->
                _sessionDates.value = sessions
                    .filter { it.finishedAt != null }
                    .map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
                    .toSet()
            }
        }
    }

    fun prevMonth() { _currentMonth.update { it.minusMonths(1) } }
    fun nextMonth() { _currentMonth.update { it.plusMonths(1) } }

    fun loadDaySessions(date: LocalDate) {
        viewModelScope.launch {
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val allSessions = sessionRepo.observeAll().first()
            val daySessions = allSessions
                .filter { it.finishedAt != null && it.startedAt in dayStart until dayEnd }
                .sortedBy { it.startedAt }

            if (daySessions.isEmpty()) {
                _selectedDaySessions.value = null
                return@launch
            }

            val items = daySessions.map { session ->
                val isCardio = session.type != "GYM"
                val workout = if (!isCardio) workoutRepo.getById(session.workoutId) else null
                val entries = if (!isCardio) sessionRepo.getEntriesForSession(session.id) else emptyList()

                val summaries = entries.mapNotNull { entry ->
                    val name = workout?.exercises?.find { it.id == entry.exerciseId }?.name
                        ?: return@mapNotNull null
                    ExerciseSummary(name, entry.sets)
                }

                var totalSets = 0
                var totalVolume = 0f
                summaries.forEach { s ->
                    totalSets += s.sets.size
                    s.sets.forEach { set ->
                        if (set is RepSetRecord) {
                            totalVolume += set.reps * set.weightKg
                        }
                    }
                }

                val startTime = timeFormat.format(Date(session.startedAt))
                val endTime = timeFormat.format(Date(session.finishedAt!!))
                val durationMin = (session.finishedAt - session.startedAt) / 60_000

                val workoutName = when {
                    session.type == "RUNNING" -> "ריצה"
                    session.type == "WALKING" -> "הליכה"
                    else -> workout?.label?.displayName ?: session.workoutId
                }

                SessionSummaryItem(
                    sessionId = session.id,
                    sessionType = session.type,
                    workoutLabel = workout?.label,
                    workoutName = workoutName,
                    timeRange = "$startTime – $endTime",
                    durationMinutes = durationMin,
                    totalSets = totalSets,
                    totalVolumeKg = totalVolume,
                    distanceKm = session.distanceMeters?.let { it / 1000.0 },
                    exerciseSummaries = summaries
                )
            }

            _selectedDaySessions.value = DaySessionsUiState(
                dateLabel = dateFormat.format(Date(daySessions.first().startedAt)),
                sessions = items
            )
        }
    }

    fun clearSelectedDay() {
        _selectedDaySessions.value = null
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            sessionRepo.deleteSession(sessionId)
            _selectedDaySessions.update { state ->
                state?.let {
                    val remaining = it.sessions.filter { s -> s.sessionId != sessionId }
                    if (remaining.isEmpty()) null else it.copy(sessions = remaining)
                }
            }
        }
    }
}
