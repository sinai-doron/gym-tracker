package com.doron.gymtracker.ui.screens.plan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"])

    private val _workout = MutableStateFlow<Workout?>(null)
    val workout: StateFlow<Workout?> = _workout.asStateFlow()

    init {
        reload()
    }

    fun save(exercise: ExerciseDefinition, isNew: Boolean) = change {
        if (isNew) workoutRepo.addExercise(workoutId, exercise) else workoutRepo.updateExercise(exercise)
    }

    fun remove(exerciseId: String) = change { workoutRepo.removeExercise(exerciseId) }

    fun move(exerciseId: String, direction: Int) = change { workoutRepo.moveExercise(workoutId, exerciseId, direction) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            _workout.value = workoutRepo.getById(workoutId)
        }
    }

    private fun reload() = change {}
}
