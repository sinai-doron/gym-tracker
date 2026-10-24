package com.doron.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.IsDeloadWeekUseCase
import com.doron.gymtracker.domain.usecase.ResolveExerciseVariantUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val workout: Workout? = null,
    val elapsedSecs: Long = 0L,
    val entries: Map<String, ExerciseEntry> = emptyMap(),
    val isFinished: Boolean = false,
    val isDeloadWeek: Boolean = false
)

class SessionViewModel @AssistedInject constructor(
    @Assisted val sessionId: Long,
    @Assisted val workoutId: String,
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val resolveVariant: ResolveExerciseVariantUseCase,
    private val isDeloadWeek: IsDeloadWeekUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sessionId: Long, workoutId: String): SessionViewModel
    }


    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isDeloadWeek = isDeloadWeek()) }
        viewModelScope.launch {
            val raw = workoutRepo.getById(workoutId)
            // Resolve A/B variant for each exercise based on current week number
            val resolved = raw?.let { w ->
                val now = System.currentTimeMillis()
                w.copy(exercises = w.exercises.map { resolveVariant(it, now) })
            }
            _uiState.update { it.copy(workout = resolved) }
        }
        viewModelScope.launch {
            while (!_uiState.value.isFinished) {
                delay(1_000)
                _uiState.update { it.copy(elapsedSecs = it.elapsedSecs + 1) }
            }
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            finishSessionUseCase(sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    fun onEntryUpdated(entry: ExerciseEntry) {
        _uiState.update { state ->
            state.copy(entries = state.entries + (entry.exerciseId to entry))
        }
    }
}
