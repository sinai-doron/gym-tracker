package com.doron.gymtracker.ui.screens.cardio

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.ActiveClock
import com.doron.gymtracker.data.Cadence
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.CardioRun
import com.doron.gymtracker.data.DistanceMark
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.Lap
import com.doron.gymtracker.data.Laps
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.Pause
import com.doron.gymtracker.data.RunRecord
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.RunBadge
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.usecase.GetPersonalRecordsUseCase
import com.doron.gymtracker.domain.usecase.GetRunBadgesUseCase
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
    val target: RunTarget = RunTarget(),
    val lapMeters: Int = 0,
    val laps: List<Lap> = emptyList(),
    val steps: Int? = null,
    val cadence: Int? = null,
    val elevationGainM: Double = 0.0,
    val weather: Weather? = null,
    val calories: Int? = null,
    val isFinished: Boolean = false,
    /** Badges this run earned for the first time, to celebrate on the finish screen. */
    val newBadges: List<RunBadge> = emptyList(),
    /** Personal records this run broke, celebrated after the badges. */
    val newRecords: List<PersonalRecord> = emptyList(),
    /** The records this run holds now, for the shared picture. */
    val heldRecords: List<PersonalRecord> = emptyList(),
    /** When each fix that moved the run happened, for auto-pause. */
    val moves: List<DistanceMark> = emptyList(),
    /** Stopped for long enough that the clock has stopped too. */
    val isPaused: Boolean = false,
    /** Pauses the runner asked for; the last is open while paused by hand. */
    val pauses: List<Pause> = emptyList(),
    /** For a route target: the route, how long it is and how far along it the runner is. */
    val plannedRoute: List<RoutePoint> = emptyList(),
    val routeLengthM: Double = 0.0,
    val routeAlongM: Double = 0.0,
    val isOffRoute: Boolean = false,
    /** The name this run was saved under as a route, once saved. */
    val savedRouteName: String? = null
) {
    val clock: ActiveClock? get() = startedAt?.let { ActiveClock(it, moves, pauses) }

    val isManuallyPaused: Boolean get() = pauses.lastOrNull()?.let { it.endedAt == null } == true

    val avgCadence: Int? get() = steps?.let { Cadence.average(it, elapsedSecs) }

    /** Target progress (1 = reached), or null with no target. */
    val targetProgress: Float?
        get() = if (routeLengthM > 0) {
            (routeAlongM / (routeLengthM - CardioRun.ROUTE_FINISH_SLACK_M).coerceAtLeast(1.0)).toFloat()
        } else target.progress(distanceMeters, elapsedSecs)

    /** The lap being run now: its number, meters so far and seconds so far. */
    val currentLapNumber: Int get() = laps.size + 1
    val currentLapMeters: Double get() = distanceMeters - laps.size * lapMeters.toDouble()
    val currentLapSecs: Long
        get() {
            val clock = clock ?: return 0
            val lapStart = laps.lastOrNull()?.endedAt ?: startedAt ?: return 0
            val now = System.currentTimeMillis()
            return (clock.activeMs(now) - clock.activeMs(lapStart)) / 1000
        }

    /** Average pace so far, seconds per km, or null before there's enough distance. */
    val avgPaceSecsPerKm: Long?
        get() = if (distanceMeters >= MIN_DISTANCE_FOR_PACE_M) (elapsedSecs * 1000 / distanceMeters).toLong() else null

    val currentPace: String
        get() {
            if (currentSpeed <= 0.3f) return ""
            val paceSecsPerKm = (1000.0 / currentSpeed).toInt()
            return "%d:%02d".format(paceSecsPerKm / 60, paceSecsPerKm % 60)
        }

    private companion object {
        const val MIN_DISTANCE_FOR_PACE_M = 50.0
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
    private val healthConnect: HealthConnectManager,
    private val getRunBadges: GetRunBadgesUseCase,
    private val getPersonalRecords: GetPersonalRecordsUseCase,
    private val routeRepo: RouteRepository,
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
                        tracks = run.tracks,
                        target = run.target,
                        lapMeters = run.lapMeters,
                        laps = run.laps,
                        steps = run.steps,
                        cadence = run.cadence,
                        elevationGainM = run.elevationGainM,
                        weather = run.weather,
                        calories = run.calories,
                        moves = run.moves,
                        pauses = run.pauses,
                        plannedRoute = run.plannedRoute,
                        routeLengthM = run.routeLengthM,
                        routeAlongM = run.routeAlongM,
                        isOffRoute = run.offRouteSince != null
                    )
                }
            }
        }

        // Elapsed time comes from the clock, so it stays right however long the screen was off
        viewModelScope.launch {
            while (!_uiState.value.isFinished) {
                _uiState.value.startedAt?.let { start ->
                    // Moving time: the clock stops while auto-paused
                    val now = System.currentTimeMillis()
                    _uiState.update { state ->
                        val clock = ActiveClock(start, state.moves, state.pauses)
                        state.copy(elapsedSecs = clock.activeMs(now) / 1000, isPaused = clock.isPaused(now))
                    }
                }
                delay(1_000)
            }
        }
    }

    fun pause() = recorder.pause()

    fun resume() = recorder.resume()

    fun finish() {
        val run = recorder.stop()
        CardioTrackingService.stop(app)
        val now = System.currentTimeMillis()
        _uiState.update { state ->
            state.copy(
                isFinished = true,
                distanceMeters = run?.distanceMeters ?: state.distanceMeters,
                route = run?.route ?: state.route,
                tracks = run?.tracks ?: state.tracks,
                // Now with the last stretch after the final full lap
                laps = run?.let { Laps.split(it.startedAt, it.route, it.lapMeters.toDouble(), it.clock, finishedAt = now, pauses = it.pauses) }
                    ?: state.laps,
                steps = run?.steps ?: state.steps,
                elevationGainM = run?.elevationGainM ?: state.elevationGainM,
                calories = run?.calories ?: state.calories,
                moves = run?.moves ?: state.moves,
                pauses = run?.pauses ?: state.pauses,
                isPaused = false,
                // The final moving time, up to the moment the run was finished
                elapsedSecs = run?.clock?.activeMs(now)?.div(1000) ?: state.elapsedSecs
            )
        }
        viewModelScope.launch {
            val final = _uiState.value
            sessionRepo.finishCardioSession(sessionId, final.distanceMeters)
            _uiState.update { it.copy(newBadges = getRunBadges.earnedBy(sessionId)) }
            if (sessionType == "RUNNING") {
                _uiState.update { it.copy(newRecords = getPersonalRecords.newBy(sessionId)) }
                _uiState.update { it.copy(heldRecords = getPersonalRecords().filter { r -> r.sessionId == sessionId }) }
            }
            sessionRepo.getRunDetails(sessionId)?.let {
                sessionRepo.saveRunDetails(
                    it.copy(steps = final.steps, elevationGainM = final.elevationGainM, calories = final.calories)
                )
            }
            final.startedAt?.let { startedAt ->
                healthConnect.writeRun(
                    RunRecord(
                        type = final.type,
                        startedAt = startedAt,
                        finishedAt = sessionRepo.getById(sessionId)?.finishedAt ?: System.currentTimeMillis(),
                        distanceMeters = final.distanceMeters,
                        elevationGainM = final.elevationGainM,
                        calories = final.calories,
                        laps = final.laps,
                        route = final.route
                    )
                )
            }
        }
    }

    /** Keeps this run's route to run again; it's named after where it starts. */
    fun saveAsRoute() {
        viewModelScope.launch {
            val points = _uiState.value.route.map { RoutePoint(it.latitude, it.longitude, it.altitude) }
            val id = routeRepo.save(points)
            _uiState.update { it.copy(savedRouteName = routeRepo.get(id)?.name) }
        }
    }

    fun recordsSeen() {
        _uiState.update { it.copy(newRecords = emptyList()) }
    }

    fun badgesSeen() {
        _uiState.update { it.copy(newBadges = emptyList()) }
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
