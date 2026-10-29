package com.doron.gymtracker.ui.screens.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.domain.model.ExerciseProgress
import com.doron.gymtracker.domain.usecase.GetExerciseProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val progress: ExerciseProgress? = null
)

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExerciseProgress: GetExerciseProgressUseCase
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _uiState = MutableStateFlow(ExerciseProgressUiState())
    val uiState: StateFlow<ExerciseProgressUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = ExerciseProgressUiState(isLoading = false, progress = getExerciseProgress(exerciseId))
        }
    }
}
