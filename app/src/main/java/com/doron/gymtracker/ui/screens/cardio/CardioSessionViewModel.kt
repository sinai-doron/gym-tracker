package com.doron.gymtracker.ui.screens.cardio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.service.CardioTrackingService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardioUiState(
    val type: String = "RUNNING",
    val startedAt: Long? = null,
    val elapsedSecs: Long = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeed: Float = 0f,
    val route: List<LocationPoint> = emptyList(),
    val tracks: List<TrackLog> = emptyList(),
    val isFinished: Boolean = false
) {
    val currentPace: String
        get() {
            if (currentSpeed <= 0.3f) return ""
            val paceSecsPerKm = (1000.0 / currentSpeed).toInt()
            return "%d:%02d".format(paceSecsPerKm / 60, paceSecsPerKm % 60)
        }
}

/**
 * Shows a run that [CardioRecorder] records in the background. Leaving this screen (or
 * the screen turning off) doesn't stop the recording; only finishing or discarding does.
 */
@HiltViewModel(assistedFactory = CardioSessionViewModel.Factory::class)
class CardioSessionViewModel @AssistedInject constructor(
    @Assisted val sessionId: Long,
    @Assisted val sessionType: String,
    private val sessionRepo: SessionRepository,
    private val recorder: CardioRecorder,
    private val app: Application,
    val settingsPrefs: SettingsPrefs
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sessionId: Long, sessionType: String): CardioSessionViewModel
    }

    private val _uiState = MutableStateFlow(CardioUiState(type = sessionType))
    val uiState: StateFlow<CardioUiState> = _uiState.asStateFlow()

    init {
        // Also covers coming back to a run the system stopped: recording picks up from the database
        if (!recorder.isRecording(sessionId)) CardioTrackingService.start(app, sessionId)

        viewModelScope.launch {
            recorder.run.collect { run ->
                if (run == null || run.sessionId != sessionId || _uiState.value.isFinished) return@collect
                _uiState.update {
                    it.copy(
                        startedAt = run.startedAt,
                        distanceMeters = run.distanceMeters,
                        currentSpeed = run.currentSpeed,
                        route = run.route,
                        tracks = run.tracks
                    )
                }
            }
        }

        // Elapsed time comes from the clock, so it stays right however long the screen was off
        viewModelScope.launch {
            while (!_uiState.value.isFinished) {
                _uiState.value.startedAt?.let { start ->
                    _uiState.update { it.copy(elapsedSecs = (System.currentTimeMillis() - start) / 1000) }
                }
                delay(1_000)
            }
        }
    }

    fun finish() {
        val run = recorder.stop()
        CardioTrackingService.stop(app)
        _uiState.update { state ->
            state.copy(
                isFinished = true,
                distanceMeters = run?.distanceMeters ?: state.distanceMeters,
                route = run?.route ?: state.route,
                tracks = run?.tracks ?: state.tracks
            )
        }
        viewModelScope.launch {
            sessionRepo.finishCardioSession(sessionId, _uiState.value.distanceMeters)
        }
    }

    /** Deletes a run/walk started by mistake. */
    fun discard(onDiscarded: () -> Unit) {
        recorder.stop()
        CardioTrackingService.stop(app)
        viewModelScope.launch {
            sessionRepo.deleteSession(sessionId)
            onDiscarded()
        }
    }
}
