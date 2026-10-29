package com.doron.gymtracker.ui.screens.cardio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.LocationTracker
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.media.NowPlayingProvider
import com.doron.gymtracker.service.CardioTrackingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardioUiState(
    val type: String = "RUNNING",
    val elapsedSecs: Long = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeed: Float = 0f,
    val currentPace: String = "",
    val route: List<LocationPoint> = emptyList(),
    val tracks: List<TrackLog> = emptyList(),
    val isFinished: Boolean = false
)

@HiltViewModel(assistedFactory = CardioSessionViewModel.Factory::class)
class CardioSessionViewModel @AssistedInject constructor(
    @Assisted val sessionId: Long,
    @Assisted val sessionType: String,
    private val sessionRepo: SessionRepository,
    private val locationTracker: LocationTracker,
    private val app: Application
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sessionId: Long, sessionType: String): CardioSessionViewModel
    }

    private val _uiState = MutableStateFlow(CardioUiState(type = sessionType))
    val uiState: StateFlow<CardioUiState> = _uiState.asStateFlow()

    init {
        locationTracker.reset()
        CardioTrackingService.start(app, sessionType)

        viewModelScope.launch {
            locationTracker.locationUpdates().collect { point ->
                if (_uiState.value.isFinished) return@collect
                sessionRepo.logLocation(sessionId, point.latitude, point.longitude, point.speed, point.altitude)
                _uiState.update { state ->
                    state.copy(route = state.route + point)
                }
            }
        }

        viewModelScope.launch {
            locationTracker.totalDistance.collect { dist ->
                _uiState.update { it.copy(distanceMeters = dist) }
                CardioTrackingService.updateDistance(dist)
            }
        }

        viewModelScope.launch {
            locationTracker.currentSpeed.collect { speed ->
                _uiState.update { it.copy(currentSpeed = speed) }
            }
        }

        viewModelScope.launch {
            locationTracker.currentPace.collect { pace ->
                _uiState.update { it.copy(currentPace = pace) }
            }
        }

        viewModelScope.launch {
            while (!_uiState.value.isFinished) {
                kotlinx.coroutines.delay(1_000)
                _uiState.update { it.copy(elapsedSecs = it.elapsedSecs + 1) }
            }
        }

        viewModelScope.launch {
            var lastTrackKey = ""
            NowPlayingProvider.state.collect { nowPlaying ->
                if (_uiState.value.isFinished) return@collect
                if (nowPlaying == null || !nowPlaying.isPlaying) return@collect
                val key = "${nowPlaying.title}|${nowPlaying.artist}"
                if (key != lastTrackKey && nowPlaying.title.isNotBlank()) {
                    lastTrackKey = key
                    sessionRepo.logTrack(sessionId, nowPlaying.title, nowPlaying.artist)
                    val tracks = sessionRepo.getTracksForSession(sessionId)
                    _uiState.update { it.copy(tracks = tracks) }
                }
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFinished = true) }
            CardioTrackingService.stop(app)
            sessionRepo.finishCardioSession(sessionId, _uiState.value.distanceMeters)
        }
    }

    /** Deletes a run/walk started by mistake. */
    fun discard(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            CardioTrackingService.stop(app)
            sessionRepo.deleteSession(sessionId)
            onDiscarded()
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!_uiState.value.isFinished) {
            CardioTrackingService.stop(app)
        }
    }
}
