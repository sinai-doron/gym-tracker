package com.doron.gymtracker.ui.screens.routes

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.Gpx
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.domain.model.SavedRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val routeRepo: RouteRepository,
    private val app: Application
) : ViewModel() {

    sealed interface Event {
        data object Imported : Event
        data object NotARoute : Event
    }

    private val _routes = MutableStateFlow<List<SavedRoute>?>(null)
    /** Null while loading. */
    val routes: StateFlow<List<SavedRoute>?> = _routes.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    fun load() {
        viewModelScope.launch { _routes.value = routeRepo.getAllWithPoints() }
    }

    fun import(uri: Uri) {
        viewModelScope.launch {
            _busy.value = true
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openInputStream(uri)!!.bufferedReader().use { Gpx.parse(it.readText()) }
                }.onFailure { Log.w("Routes", "Couldn't read the file", it) }.getOrNull()
            }
            if (parsed == null || parsed.points.size < 2) {
                _event.value = Event.NotARoute
            } else {
                // Named after where it starts, like routes saved from runs; renaming is a tap away
                routeRepo.save(parsed.points)
                _event.value = Event.Imported
                load()
            }
            _busy.value = false
        }
    }

    fun eventHandled() {
        _event.value = null
    }
}
