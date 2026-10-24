package com.doron.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseDetailUiState(
    val exercise: ExerciseDefinition,
    val completedSets: List<SetRecord> = emptyList(),
    val restCountdownSecs: Int? = null,
    val timerRunning: Boolean = false,
    val timerElapsedSecs: Int = 0
)

class ExerciseDetailViewModel(
    private val sessionId: Long,
    exercise: ExerciseDefinition,
    private val logSet: LogSetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState(exercise))
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    private var restTimerJob: Job? = null
    private var setTimerJob: Job? = null

    fun logRepSet(reps: Int, weightKg: Float) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercise
            val record = RepSetRecord(
                id = 0L, entryId = 0L,
                completedAt = System.currentTimeMillis(),
                reps = reps, weightKg = weightKg
            )
            logSet(sessionId, exercise.id, record)
            _uiState.update { it.copy(completedSets = it.completedSets + record) }
            startRestTimer(exercise.restDurationSecs)
        }
    }

    fun startTimedSet() {
        if (_uiState.value.timerRunning) return
        _uiState.update { it.copy(timerRunning = true, timerElapsedSecs = 0) }
        setTimerJob = viewModelScope.launch {
            while (_uiState.value.timerRunning) {
                delay(1_000)
                _uiState.update { it.copy(timerElapsedSecs = it.timerElapsedSecs + 1) }
            }
        }
    }

    fun stopTimedSet() {
        if (!_uiState.value.timerRunning) return
        setTimerJob?.cancel()
        setTimerJob = null
        val elapsed = _uiState.value.timerElapsedSecs.coerceAtLeast(1)
        val exercise = _uiState.value.exercise
        val record = TimedSetRecord(
            id = 0L, entryId = 0L,
            completedAt = System.currentTimeMillis(),
            durationSecs = elapsed
        )
        // Update state synchronously so UI reflects the set immediately
        _uiState.update { it.copy(timerRunning = false, timerElapsedSecs = 0, completedSets = it.completedSets + record) }
        viewModelScope.launch {
            logSet(sessionId, exercise.id, record)
            startRestTimer(exercise.restDurationSecs)
        }
    }

    fun dismissRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        _uiState.update { it.copy(restCountdownSecs = null) }
    }

    private fun startRestTimer(durationSecs: Int) {
        restTimerJob?.cancel()
        _uiState.update { it.copy(restCountdownSecs = durationSecs) }
        restTimerJob = viewModelScope.launch {
            var remaining = durationSecs
            while (remaining > 0) {
                delay(1_000)
                remaining--
                _uiState.update { it.copy(restCountdownSecs = if (remaining > 0) remaining else null) }
            }
        }
    }
}
