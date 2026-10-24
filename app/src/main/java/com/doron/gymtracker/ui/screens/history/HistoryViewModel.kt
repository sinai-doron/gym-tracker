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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

data class TrendsUiState(
    val exerciseNames: List<String> = emptyList(),
    val selectedExercise: String? = null,
    val dataPoints: List<TrendPoint> = emptyList()
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    private val _lastSession = MutableStateFlow(LastSessionUiState())
    val lastSession: StateFlow<LastSessionUiState> = _lastSession.asStateFlow()

    private val _trends = MutableStateFlow(TrendsUiState())
    val trends: StateFlow<TrendsUiState> = _trends.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    init {
        viewModelScope.launch { loadLastSession() }
        viewModelScope.launch {
            sessionRepo.observeAll().collectLatest { sessions ->
                val exerciseNames = sessions
                    .flatMap { sessionRepo.getEntriesForSession(it.id) }
                    .map { it.exerciseId }
                    .distinct()
                    .mapNotNull { id ->
                        sessions.firstOrNull()?.let { s ->
                            workoutRepo.getById(s.workoutId)?.exercises?.find { it.id == id }?.name
                        }
                    }
                _trends.update { it.copy(exerciseNames = exerciseNames) }
                recomputeTrends(sessions)
            }
        }
    }

    private suspend fun loadLastSession() {
        val session = sessionRepo.getLastCompleted() ?: return
        val workout = workoutRepo.getById(session.workoutId)
        val entries = sessionRepo.getEntriesForSession(session.id)
        val summaries = entries.mapNotNull { entry ->
            val exerciseName = workout?.exercises?.find { it.id == entry.exerciseId }?.name
                ?: return@mapNotNull null
            ExerciseSummary(exerciseName, entry.sets)
        }
        _lastSession.update {
            it.copy(
                workoutLabel = workout?.label,
                dateLabel = dateFormat.format(Date(session.startedAt)),
                summaries = summaries
            )
        }
    }

    fun selectExerciseForTrend(name: String) {
        _trends.update { it.copy(selectedExercise = name) }
        viewModelScope.launch {
            val sessions = sessionRepo.observeAll().let { flow ->
                var result = emptyList<com.doron.gymtracker.domain.model.Session>()
                flow.collectLatest { result = it }
                result
            }
            recomputeTrends(sessions)
        }
    }

    private suspend fun recomputeTrends(sessions: List<com.doron.gymtracker.domain.model.Session>) {
        val selectedName = _trends.value.selectedExercise ?: return
        val points = sessions
            .sortedBy { it.startedAt }
            .mapNotNull { session ->
                val workout = workoutRepo.getById(session.workoutId) ?: return@mapNotNull null
                val exerciseDef = workout.exercises.find { it.name == selectedName } ?: return@mapNotNull null
                val entries = sessionRepo.getEntriesForSession(session.id)
                val entry = entries.find { it.exerciseId == exerciseDef.id } ?: return@mapNotNull null
                val maxValue = entry.sets.mapNotNull { set ->
                    when (set) {
                        is RepSetRecord -> set.weightKg
                        is TimedSetRecord -> set.durationSecs.toFloat()
                    }
                }.maxOrNull() ?: return@mapNotNull null
                TrendPoint(
                    dateLabel = dateFormat.format(Date(session.startedAt)),
                    value = maxValue
                )
            }
        _trends.update { it.copy(dataPoints = points) }
    }
}
