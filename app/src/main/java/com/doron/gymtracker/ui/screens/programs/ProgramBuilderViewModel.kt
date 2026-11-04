package com.doron.gymtracker.ui.screens.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.ProgramRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ProgramBuilderUiState(
    val program: Program? = null,
    /** Its workouts in rotation order, hidden ones left out. */
    val workouts: List<Workout> = emptyList()
)

@HiltViewModel
class ProgramBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val programs: ProgramRepository,
    private val workouts: WorkoutRepository
) : ViewModel() {

    private val programId: String = checkNotNull(savedStateHandle["programId"])

    private val _state = MutableStateFlow(ProgramBuilderUiState())
    val state: StateFlow<ProgramBuilderUiState> = _state.asStateFlow()

    /** Also on returning from a workout's exercises, whose count and length may have changed. */
    fun reload() = change {}

    fun rename(name: String) = change { programs.rename(programId, name.trim()) }

    fun setStartDate(date: LocalDate) = change { programs.setStartDate(programId, date) }

    fun addWorkout(code: String, name: String) = change { workouts.addWorkout(programId, code.trim(), name.trim().ifEmpty { null }) }

    fun editWorkout(id: String, code: String, name: String) = change { workouts.editWorkout(id, code.trim(), name.trim().ifEmpty { null }) }

    fun moveWorkout(id: String, direction: Int) = change { workouts.moveWorkout(programId, id, direction) }

    fun removeWorkout(id: String) = change { workouts.removeWorkout(id) }

    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            _state.value = ProgramBuilderUiState(programs.get(programId), workouts.getForProgram(programId))
        }
    }
}
