package com.doron.gymtracker.ui.screens.workouts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.WorkoutPreview
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import com.doron.gymtracker.domain.usecase.GetWorkoutPreviewUseCase
import com.doron.gymtracker.domain.usecase.StartSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutsUiState(
    val previews: List<WorkoutPreview> = emptyList(),
    val nextWorkoutId: String? = null,
    val selectedIndex: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val workoutRepo: WorkoutRepository,
    private val getNextWorkout: GetNextWorkoutUseCase,
    private val getWorkoutPreview: GetWorkoutPreviewUseCase,
    private val startSession: StartSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutsUiState())
    val uiState: StateFlow<WorkoutsUiState> = _uiState.asStateFlow()

    fun reload() {
        viewModelScope.launch {
            val nextId = getNextWorkout().id
            val previews = workoutRepo.getAll().sortedBy { it.label }.map { getWorkoutPreview(it) }
            _uiState.update {
                it.copy(
                    previews = previews,
                    nextWorkoutId = nextId,
                    // First load opens on the upcoming workout; later reloads keep the user's pick
                    selectedIndex = if (it.isLoading) {
                        previews.indexOfFirst { p -> p.workout.id == nextId }.coerceAtLeast(0)
                    } else it.selectedIndex,
                    isLoading = false
                )
            }
        }
    }

    fun select(index: Int) {
        _uiState.update { it.copy(selectedIndex = index) }
    }

    suspend fun startSelected(): Pair<Long, String>? {
        val workout = _uiState.value.previews.getOrNull(_uiState.value.selectedIndex)?.workout ?: return null
        return startSession(workout.id) to workout.id
    }
}
