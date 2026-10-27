package com.doron.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.domain.usecase.GetLastWeightUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.ProgressionHintUseCase
import com.doron.gymtracker.domain.usecase.UpdateSetUseCase
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
    val restTotalSecs: Int = 0,
    val timerRunning: Boolean = false,
    val timerElapsedSecs: Int = 0,
    val progressionHint: ProgressionHint = ProgressionHint.OnTrack,
    val lastWeight: Float? = null,
    val editingSetIndex: Int? = null
)

class ExerciseDetailViewModel(
    private val sessionId: Long,
    exercise: ExerciseDefinition,
    private val logSet: LogSetUseCase,
    private val updateSet: UpdateSetUseCase? = null,
    private val progressionHint: ProgressionHintUseCase? = null,
    private val getLastWeight: GetLastWeightUseCase? = null,
    restTimer: RestTimer? = null,
    private val onTargetSetsChanged: (exerciseId: String, sets: Int) -> Unit = { _, _ -> }
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseDetailUiState(exercise))
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    private val restTimer = restTimer ?: RestTimer(viewModelScope)
    private var setTimerJob: Job? = null

    init {
        // Mirror the session-wide rest timer so every exercise sheet shows the same countdown
        viewModelScope.launch {
            this@ExerciseDetailViewModel.restTimer.state.collect { rest ->
                _uiState.update {
                    it.copy(restCountdownSecs = rest?.remainingSecs, restTotalSecs = rest?.totalSecs ?: 0)
                }
            }
        }
        val ex = _uiState.value.exercise
        if (ex is RepExerciseDefinition) {
            viewModelScope.launch {
                val hint = progressionHint?.invoke(ex.id, ex.targetReps) ?: ProgressionHint.OnTrack
                val lastWeight = getLastWeight?.invoke(ex.id)
                _uiState.update { it.copy(progressionHint = hint, lastWeight = lastWeight) }
            }
        }
    }

    fun logRepSet(reps: Int, weightKg: Float) {
        viewModelScope.launch {
            val exercise = _uiState.value.exercise
            val record = RepSetRecord(
                id = 0L, entryId = 0L,
                completedAt = System.currentTimeMillis(),
                reps = reps, weightKg = weightKg
            )
            val dbId = logSet(sessionId, exercise.id, record)
            val saved = record.copy(id = dbId)
            _uiState.update { it.copy(completedSets = it.completedSets + saved, lastWeight = weightKg) }
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
        _uiState.update { it.copy(timerRunning = false, timerElapsedSecs = 0) }
        viewModelScope.launch {
            val dbId = logSet(sessionId, exercise.id, record)
            val saved = record.copy(id = dbId)
            _uiState.update { it.copy(completedSets = it.completedSets + saved) }
            startRestTimer(exercise.restDurationSecs)
        }
    }

    fun logDistanceSet(distanceM: Int) {
        val exercise = _uiState.value.exercise
        val record = DistanceSetRecord(
            id = 0L, entryId = 0L,
            completedAt = System.currentTimeMillis(),
            distanceM = distanceM
        )
        viewModelScope.launch {
            val dbId = logSet(sessionId, exercise.id, record)
            val saved = record.copy(id = dbId)
            _uiState.update { it.copy(completedSets = it.completedSets + saved) }
            startRestTimer(exercise.restDurationSecs)
        }
    }

    fun startEditing(index: Int) {
        _uiState.update { it.copy(editingSetIndex = index) }
    }

    fun cancelEditing() {
        _uiState.update { it.copy(editingSetIndex = null) }
    }

    fun updateRepSet(index: Int, reps: Int, weightKg: Float) {
        viewModelScope.launch {
            val sets = _uiState.value.completedSets.toMutableList()
            val old = sets[index] as? RepSetRecord ?: return@launch
            val updated = old.copy(reps = reps, weightKg = weightKg)
            updateSet?.invoke(updated)
            sets[index] = updated
            _uiState.update { it.copy(completedSets = sets, editingSetIndex = null, lastWeight = weightKg) }
        }
    }

    fun updateDistanceSet(index: Int, distanceM: Int) {
        viewModelScope.launch {
            val sets = _uiState.value.completedSets.toMutableList()
            val old = sets[index] as? DistanceSetRecord ?: return@launch
            val updated = old.copy(distanceM = distanceM)
            updateSet?.invoke(updated)
            sets[index] = updated
            _uiState.update { it.copy(completedSets = sets, editingSetIndex = null) }
        }
    }

    fun dismissRestTimer() = restTimer.dismiss()

    fun adjustRestTimer(deltaSecs: Int) = restTimer.adjust(deltaSecs)

    private fun startRestTimer(durationSecs: Int) = restTimer.start(durationSecs)

    fun changeTargetSets(delta: Int) {
        val exercise = _uiState.value.exercise
        val sets = (exercise.targetSets + delta).coerceIn(1, 10)
        if (sets == exercise.targetSets) return
        val updated = when (exercise) {
            is RepExerciseDefinition -> exercise.copy(targetSets = sets)
            is TimedExerciseDefinition -> exercise.copy(targetSets = sets)
            is DistanceExerciseDefinition -> exercise.copy(targetSets = sets)
        }
        _uiState.update { it.copy(exercise = updated) }
        onTargetSetsChanged(exercise.id, sets)
    }
}
