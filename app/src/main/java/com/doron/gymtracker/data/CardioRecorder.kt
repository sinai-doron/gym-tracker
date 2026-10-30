package com.doron.gymtracker.data

import android.util.Log
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RunDetails
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.media.NowPlayingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** A run or walk while it's being recorded. */
data class CardioRun(
    val sessionId: Long,
    val type: String,
    val startedAt: Long,
    val distanceMeters: Double = 0.0,
    val currentSpeed: Float = 0f,
    val route: List<LocationPoint> = emptyList(),
    val tracks: List<TrackLog> = emptyList(),
    val target: RunTarget = RunTarget(),
    /** 0 when laps are off. */
    val lapMeters: Int = 0,
    val laps: List<Lap> = emptyList(),
    /** Null when the phone can't count steps (no sensor or no permission). */
    val steps: Int? = null,
    /** Steps per minute over the last half minute. */
    val cadence: Int? = null,
    val elevationGainM: Double = 0.0,
    val weather: Weather? = null,
    /** For calories; null until set in settings. */
    val bodyWeightKg: Float? = null,
    /** When each fix that moved the run happened: what auto-pause is worked out from. */
    val moves: List<DistanceMark> = emptyList(),
    /** The route being followed, for a route target. */
    val plannedRoute: List<RoutePoint> = emptyList(),
    val routeLengthM: Double = 0.0,
    /** How far along the planned route the runner has got. */
    val routeAlongM: Double = 0.0,
    /** Since when the runner has been off the planned route, or null while on it. */
    val offRouteSince: Long? = null
) {
    val clock: ActiveClock get() = ActiveClock(startedAt, moves)

    /** 1 once the route is done; null without a route. The last few meters don't need running. */
    val routeProgress: Float?
        get() = if (routeLengthM > 0) (routeAlongM / (routeLengthM - ROUTE_FINISH_SLACK_M).coerceAtLeast(1.0)).toFloat() else null

    companion object {
        const val ROUTE_FINISH_SLACK_M = 30.0
    }

    val calories: Int?
        get() = bodyWeightKg?.let { RunCalories.estimate(type, distanceMeters, it) }
}

/**
 * Records a run: GPS fixes, distance, laps, climb, steps and the songs played. It lives as long as
 * CardioTrackingService (a foreground service), not the screen, so recording carries on
 * with the screen off or the app in the background. Every fix is saved as it arrives,
 * so a run the system killed can be picked back up from the database.
 */
@Singleton
class CardioRecorder @Inject constructor(
    private val locationTracker: LocationTracker,
    private val sessionRepo: SessionRepository,
    private val settingsPrefs: SettingsPrefs,
    private val stepCounter: StepCounterManager,
    private val weatherRepo: WeatherRepository,
    private val routeRepo: RouteRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _run = MutableStateFlow<CardioRun?>(null)
    val run: StateFlow<CardioRun?> = _run.asStateFlow()

    private var job: Job? = null
    // Last fix that counted towards the distance; small moves are measured from it
    private var anchor: LocationPoint? = null
    // Recent step counter readings, for the current cadence
    private val stepSamples = ArrayDeque<StepSample>()
    // Follows the runner along the planned route, for a route target
    private var follower: RouteFollower? = null

    fun isRecording(sessionId: Long): Boolean =
        job?.isActive == true && _run.value?.sessionId == sessionId

    /** Starts recording [sessionId], continuing from whatever was already saved for it. */
    fun start(sessionId: Long) {
        if (isRecording(sessionId)) return
        job?.cancel()
        job = scope.launch {
            val session = sessionRepo.getById(sessionId) ?: return@launch
            val saved = savedRoute(sessionId)
            anchor = saved.lastOrNull()
            stepSamples.clear()
            // A new run takes the target and lap length chosen in settings; a resumed one keeps its own
            val details = sessionRepo.getRunDetails(sessionId)
                ?: RunDetails(sessionId, settingsPrefs.runTarget.first(), settingsPrefs.lapDistanceM.first())
                    .also { sessionRepo.saveRunDetails(it) }
            val moves = RouteDistance.marks(saved)
            val planned = details.target.takeIf { it.type == RunTargetType.ROUTE }?.routeId?.let { routeRepo.get(it) }
            follower = planned?.let { RouteFollower(it.points) }
            // Picking a run back up: replay it, so the follower knows how far along the route it got
            val routeAlong = saved.mapNotNull { follower?.locate(it.latitude, it.longitude) }
                .filter { it.offMeters <= RouteFollower.ON_ROUTE_M }.maxOfOrNull { it.alongMeters } ?: 0.0
            _run.value = CardioRun(
                sessionId, session.type, session.startedAt,
                distanceMeters = RouteDistance.total(saved),
                route = saved,
                tracks = sessionRepo.getTracksForSession(sessionId),
                target = details.target,
                lapMeters = details.lapMeters,
                laps = Laps.split(session.startedAt, saved, details.lapMeters.toDouble(), ActiveClock(session.startedAt, moves)),
                steps = details.steps,
                elevationGainM = Elevation.gain(saved),
                weather = details.weatherCondition?.let { Weather(details.weatherTempC ?: 0, 0, 0, it) },
                bodyWeightKg = settingsPrefs.bodyWeightKg.first(),
                moves = moves,
                plannedRoute = planned?.points ?: emptyList(),
                routeLengthM = follower?.lengthMeters ?: 0.0,
                routeAlongM = routeAlong
            )
            if (details.weatherCondition == null) launch { recordWeather(sessionId) }
            launch { countSteps(sessionId) }
            launch { locationTracker.locationUpdates().collect(::onFix) }
            launch { logTracks(sessionId) }
        }
    }

    /** Stops recording and returns the run as it stood. */
    fun stop(): CardioRun? {
        job?.cancel()
        job = null
        anchor = null
        stepSamples.clear()
        follower = null
        return _run.value.also { _run.value = null }
    }

    /**
     * Deals with a run left open by a crash or a killed app: returns it to be resumed when
     * it was active recently, otherwise closes it at its last fix and returns null.
     */
    suspend fun recoverOpenRun(now: Long = System.currentTimeMillis()): CardioRun? {
        val open = sessionRepo.getOpenCardioSession() ?: return null
        if (isRecording(open.id)) return _run.value
        val route = savedRoute(open.id)
        val lastActivity = route.lastOrNull()?.timestamp ?: open.startedAt
        if (now - lastActivity < RESUME_WINDOW_MS) return CardioRun(open.id, open.type, open.startedAt)
        Log.i(TAG, "Closing abandoned run ${open.id}")
        sessionRepo.finishCardioSession(open.id, RouteDistance.total(route), finishedAt = lastActivity)
        return null
    }

    private suspend fun onFix(point: LocationPoint) {
        val run = _run.value ?: return
        if (!RouteDistance.isUsable(point)) return
        sessionRepo.logLocation(run.sessionId, point)
        val from = anchor
        val added = if (from == null) 0.0 else RouteDistance.step(from, point)
        if (from == null || added > 0) anchor = point
        _run.update {
            if (it == null) return@update null
            val route = it.route + point
            val moved = from == null || added > 0
            val moves = if (moved) it.moves + DistanceMark(point.timestamp, it.distanceMeters + added) else it.moves
            val onRoute = follower?.locate(point.latitude, point.longitude)
            it.copy(
                moves = moves,
                routeAlongM = onRoute?.takeIf { p -> p.offMeters <= RouteFollower.ON_ROUTE_M }
                    ?.let { p -> maxOf(it.routeAlongM, p.alongMeters) } ?: it.routeAlongM,
                // Off once clearly away from the route; back on once clearly close again
                offRouteSince = when {
                    onRoute == null -> null
                    onRoute.offMeters > RouteFollower.ON_ROUTE_M -> it.offRouteSince ?: point.timestamp
                    onRoute.offMeters < BACK_ON_ROUTE_M -> null
                    else -> it.offRouteSince
                },
                distanceMeters = it.distanceMeters + added,
                // Pace over the last few seconds of route is steadier than a single fix's speed,
                // and some fixes report 0 or nothing at all
                currentSpeed = RouteDistance.recentSpeed(it.route, point),
                route = route,
                elevationGainM = if (point.altitude != null) Elevation.gain(route) else it.elevationGainM,
                // Only recut the laps when this fix could have finished one
                laps = if (added > 0 && it.lapMeters > 0) {
                    Laps.split(it.startedAt, route, it.lapMeters.toDouble(), ActiveClock(it.startedAt, moves))
                } else it.laps
            )
        }
    }

    /** Steps since the run started, from the phone's step counter (which counts since boot). */
    private suspend fun countSteps(sessionId: Long) {
        stepCounter.counterUpdates().collect { counter ->
            val details = sessionRepo.getRunDetails(sessionId) ?: return@collect
            val stepsSoFar = _run.value?.steps ?: 0
            // The first reading of a run sets the start; a reboot resets the counter, so carry on from it
            val start = details.stepCounterAtStart?.takeIf { counter >= it } ?: (counter - stepsSoFar)
            if (start != details.stepCounterAtStart) sessionRepo.saveRunDetails(details.copy(stepCounterAtStart = start))

            val now = System.currentTimeMillis()
            stepSamples.addLast(StepSample(now, counter))
            while (stepSamples.isNotEmpty() && now - stepSamples.first().timestamp > Cadence.WINDOW_MS) {
                stepSamples.removeFirst()
            }
            _run.update {
                it?.copy(steps = (counter - start).toInt(), cadence = Cadence.current(stepSamples.toList(), now))
            }
        }
    }

    /** The weather where the run started, kept with the run. */
    private suspend fun recordWeather(sessionId: Long) {
        val weather = weatherRepo.currentWeather() ?: return
        sessionRepo.getRunDetails(sessionId)?.let {
            sessionRepo.saveRunDetails(it.copy(weatherTempC = weather.temperatureC, weatherCondition = weather.condition))
        }
        _run.update { it?.copy(weather = weather) }
    }

    private suspend fun logTracks(sessionId: Long) {
        var lastTrackKey = _run.value?.tracks?.lastOrNull()?.let { "${it.title}|${it.artist}" } ?: ""
        NowPlayingProvider.state.collect { nowPlaying ->
            if (nowPlaying == null || !nowPlaying.isPlaying || nowPlaying.title.isBlank()) return@collect
            val key = "${nowPlaying.title}|${nowPlaying.artist}"
            if (key == lastTrackKey) return@collect
            lastTrackKey = key
            sessionRepo.logTrack(sessionId, nowPlaying.title, nowPlaying.artist)
            val tracks = sessionRepo.getTracksForSession(sessionId)
            _run.update { it?.copy(tracks = tracks) }
        }
    }

    private suspend fun savedRoute(sessionId: Long): List<LocationPoint> =
        sessionRepo.getRouteForSession(sessionId)

    private companion object {
        const val TAG = "CardioRecorder"
        const val RESUME_WINDOW_MS = 30 * 60_000L
        const val BACK_ON_ROUTE_M = 30.0
    }
}
