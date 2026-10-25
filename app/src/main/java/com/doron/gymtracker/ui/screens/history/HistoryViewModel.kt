package com.doron.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
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

data class TrendPoint(val dateLabel: String, val value: Float)

data class LastSessionUiState(
    val workoutLabel: WorkoutLabel? = null,
    val dateLabel: String? = null,
    val summaries: List<ExerciseSummary> = emptyList()
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

    private val _selectedDaySession = MutableStateFlow<LastSessionUiState?>(null)
    val selectedDaySession: StateFlow<LastSessionUiState?> = _selectedDaySession.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
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

    fun loadDaySession(date: LocalDate) {
        viewModelScope.launch {
            val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val sessions = sessionRepo.observeAll().first()
            val session = sessions
                .filter { it.finishedAt != null }
                .firstOrNull { it.startedAt in dayStart until dayEnd }
            if (session == null) { _selectedDaySession.value = null; return@launch }
            val workout = workoutRepo.getById(session.workoutId)
            val entries = sessionRepo.getEntriesForSession(session.id)
            val summaries = entries.mapNotNull { entry ->
                val name = workout?.exercises?.find { it.id == entry.exerciseId }?.name
                    ?: return@mapNotNull null
                ExerciseSummary(name, entry.sets)
            }
            _selectedDaySession.value = LastSessionUiState(
                workoutLabel = workout?.label,
                dateLabel = dateFormat.format(Date(session.startedAt)),
                summaries = summaries
            )
        }
    }

    fun clearSelectedDay() { _selectedDaySession.value = null }
}
