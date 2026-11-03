package com.doron.gymtracker.ui.screens.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.ProgramRepository
import com.doron.gymtracker.domain.model.ProgramSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repo: ProgramRepository
) : ViewModel() {

    private val _programs = MutableStateFlow<List<ProgramSummary>?>(null)
    /** Null until loaded; the active programme first. */
    val programs: StateFlow<List<ProgramSummary>?> = _programs.asStateFlow()

    fun reload() = change {}

    fun makeActive(id: String) = change { repo.makeActive(id) }

    fun archive(id: String) = change { repo.archive(id) }

    fun restore(id: String) = change { repo.restore(id) }

    fun delete(id: String) = change { repo.delete(id) }

    /** Makes an empty programme starting today and hands its id to [onCreated], to open it in the builder. */
    private fun change(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            _programs.value = repo.getAll()
        }
    }
}
