package com.doron.gymtracker.ui.screens.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.ElevationPoint
import com.doron.gymtracker.data.RouteFollower
import com.doron.gymtracker.data.RouteNamer
import com.doron.gymtracker.data.Slope
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.domain.model.SavedRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToLong

data class RouteDetailState(
    val route: SavedRoute,
    /** Empty when the route has no altitudes (some GPX files don't). */
    val profile: List<ElevationPoint>,
    /** Which slopes the map shows, for its legend. */
    val slopes: Set<Slope>,
    val climbM: Double?,
    val descentM: Double?,
    val highestM: Double?,
    val lowestM: Double?,
    /** At the pace of recent runs, or [DEFAULT_PACE_SECS_PER_KM] without any. */
    val estimatedSecs: Long,
    val paceSecsPerKm: Long,
    val paceFromRuns: Boolean,
    /** Street names where it starts and finishes, once looked up. */
    val startStreet: String? = null,
    val finishStreet: String? = null,
    val isLoop: Boolean
)

@HiltViewModel
class RouteDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepo: RouteRepository,
    private val sessionRepo: SessionRepository,
    private val settingsPrefs: SettingsPrefs,
    private val namer: RouteNamer
) : ViewModel() {

    private val routeId: Long = savedStateHandle.get<String>("routeId")?.toLongOrNull() ?: -1L

    private val _state = MutableStateFlow<RouteDetailState?>(null)
    /** Null while loading. */
    val state: StateFlow<RouteDetailState?> = _state.asStateFlow()

    private val _gone = MutableStateFlow(false)
    /** The route was deleted (or never existed): leave the page. */
    val gone: StateFlow<Boolean> = _gone.asStateFlow()

    init {
        viewModelScope.launch {
            val route = routeRepo.get(routeId)
            if (route == null) {
                _gone.value = true
                return@launch
            }
            val altitudes = route.points.mapNotNull { it.elevationM }.takeIf { it.size == route.points.size }
            val profile = Elevation.routeProfile(route.points)
            val (pace, fromRuns) = recentPace()
            val isLoop = route.points.size >= 2 &&
                RouteFollower.meters(route.points.first(), route.points.last()) < LOOP_M
            _state.value = RouteDetailState(
                route = route,
                profile = profile,
                slopes = Elevation.slopes(route.points).map { it.first }.toSet(),
                climbM = altitudes?.let(Elevation::climb),
                descentM = altitudes?.let(Elevation::descent),
                highestM = profile.maxOfOrNull { it.altitudeM },
                lowestM = profile.minOfOrNull { it.altitudeM },
                estimatedSecs = (route.distanceMeters / 1000 * pace).roundToLong(),
                paceSecsPerKm = pace,
                paceFromRuns = fromRuns,
                isLoop = isLoop
            )
            // Street names come from the network, so the page shows without them first
            val start = route.points.firstOrNull()?.let { namer.streetName(it) }
            _state.update { it?.copy(startStreet = start) }
            if (!isLoop) {
                val finish = route.points.lastOrNull()?.let { namer.streetName(it) }
                _state.update { it?.copy(finishStreet = finish) }
            }
        }
    }

    /** Seconds per km over the latest runs of at least a km, and whether there were any. */
    private suspend fun recentPace(): Pair<Long, Boolean> {
        val runs = sessionRepo.getCompletedSince(System.currentTimeMillis() - PACE_WINDOW_MS)
            .filter { it.type == "RUNNING" && it.finishedAt != null && (it.distanceMeters ?: 0.0) >= MIN_RUN_M }
            .sortedByDescending { it.startedAt }
            .take(RECENT_RUNS)
        if (runs.isEmpty()) return DEFAULT_PACE_SECS_PER_KM to false
        val secs = runs.sumOf { (it.finishedAt!! - it.startedAt) / 1000.0 }
        val km = runs.sumOf { it.distanceMeters!! } / 1000
        return (secs / km).roundToLong() to true
    }

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            routeRepo.rename(routeId, name)
            _state.update { it?.copy(route = it.route.copy(name = name.trim())) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            routeRepo.delete(routeId)
            // Don't leave the next run aimed at a route that's gone
            val target = settingsPrefs.runTarget.first()
            if (target.routeId == routeId) settingsPrefs.setRunTarget(target.copy(type = RunTargetType.NONE, routeId = null))
            _gone.value = true
        }
    }

    /** Makes this route the next run's target. */
    fun runRoute(onReady: () -> Unit) {
        viewModelScope.launch {
            settingsPrefs.setRunTarget(settingsPrefs.runTarget.first().copy(type = RunTargetType.ROUTE, routeId = routeId))
            onReady()
        }
    }

    companion object {
        const val DEFAULT_PACE_SECS_PER_KM = 360L
        private const val PACE_WINDOW_MS = 90L * 24 * 60 * 60 * 1000
        private const val MIN_RUN_M = 1000.0
        private const val RECENT_RUNS = 10
        private const val LOOP_M = 50.0
    }
}
