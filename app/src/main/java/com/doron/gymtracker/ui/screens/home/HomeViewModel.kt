package com.doron.gymtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val suggestedWorkout: Workout? = null,
    val selectedWorkout: Workout? = null,
    val allWorkouts: List<Workout> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getNextWorkout: GetNextWorkoutUseCase,
    private val startSession: StartSessionUseCase,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val suggested = getNextWorkout()
            val all = workoutRepo.getAll()
            _uiState.update { it.copy(suggestedWorkout = suggested, selectedWorkout = suggested, allWorkouts = all, isLoading = false) }
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
}
