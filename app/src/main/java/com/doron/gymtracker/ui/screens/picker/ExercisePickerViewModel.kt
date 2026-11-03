package com.doron.gymtracker.ui.screens.picker

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.catalogue.BodyRegions
import com.doron.gymtracker.data.repository.ExerciseLibrary
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.LibraryExercise
import com.doron.gymtracker.domain.model.LibrarySearch
import com.doron.gymtracker.domain.model.Workout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class PickerUiState(
    val workout: Workout? = null,
    /** The whole library, null until loaded. */
    val all: List<LibraryExercise>? = null,
    val recent: List<String> = emptyList(),
    val query: String = "",
    val muscles: Set<String> = emptySet(),
    val equipment: Set<String> = emptySet(),
) {
    val results: List<LibraryExercise>
        get() = all?.let { LibrarySearch.filter(it, query, muscles, equipment, recent) }.orEmpty()

    /** With no search or filter, the exercises done lately lead the list. */
    val showingRecent: Boolean get() = query.isBlank() && muscles.isEmpty() && equipment.isEmpty() && recent.isNotEmpty()
}

@HiltViewModel
class ExercisePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val library: ExerciseLibrary,
    private val workouts: WorkoutRepository
) : ViewModel() {

    private val workoutId: String = checkNotNull(savedStateHandle["workoutId"])

    private val _state = MutableStateFlow(PickerUiState())
    val state: StateFlow<PickerUiState> = _state.asStateFlow()

    private var lang: String? = null

    /** Loads the library named in [lang]; again only when the language changes. */
    fun load(lang: String) {
        if (this.lang == lang) return
        this.lang = lang
        viewModelScope.launch {
            val all = library.all(lang)
            val recent = library.recentlyDone()
            _state.update { it.copy(workout = workouts.getById(workoutId), all = all, recent = recent) }
        }
    }

    fun setQuery(text: String) = _state.update { it.copy(query = text) }

    fun toggleMuscle(slug: String) = _state.update { it.copy(muscles = it.muscles.toggle(slug)) }

    /** A tap on the body map: its region's muscles, all on or all off. */
    fun toggleRegion(muscles: List<String>) = _state.update { it.copy(muscles = BodyRegions.toggle(it.muscles, muscles)) }

    fun toggleEquipment(slug: String) = _state.update { it.copy(equipment = it.equipment.toggle(slug)) }

    /** [exercise] as a new row of the workout, ready for the targets sheet. */
    fun draft(exercise: LibraryExercise): ExerciseDefinition = exercise.toDefinition("custom_${UUID.randomUUID()}")

    /** Adds the row, linked to the library exercise it is, then [onDone]. */
    fun add(definition: ExerciseDefinition, libraryId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            workouts.addExercise(workoutId, definition, libraryId)
            onDone()
        }
    }

    /** Exercises already in the library with a name like [name], to offer before making a copy. */
    fun similar(name: String): List<LibraryExercise> = LibrarySearch.similar(_state.value.all.orEmpty(), name)

    /** Saves a user's own exercise, then adds it to the workout with its default targets. */
    fun createAndAdd(name: String, preset: String, equipment: String, muscle: String, onDone: () -> Unit) {
        val lang = lang ?: return
        viewModelScope.launch {
            val id = library.createCustom(name, lang, preset, equipment, muscle)
            val created = library.all(lang).first { it.id == id }
            workouts.addExercise(workoutId, draft(created), id)
            onDone()
        }
    }

    private fun Set<String>.toggle(slug: String) = if (slug in this) this - slug else this + slug
}
