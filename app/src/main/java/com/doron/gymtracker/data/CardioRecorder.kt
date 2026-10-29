package com.doron.gymtracker.data

import android.util.Log
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.media.NowPlayingProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val tracks: List<TrackLog> = emptyList()
)

/**
 * Records a run: GPS fixes, distance and the songs played. It lives as long as
 * CardioTrackingService (a foreground service), not the screen, so recording carries on
 * with the screen off or the app in the background. Every fix is saved as it arrives,
 * so a run the system killed can be picked back up from the database.
 */
@Singleton
class CardioRecorder @Inject constructor(
    private val locationTracker: LocationTracker,
    private val sessionRepo: SessionRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _run = MutableStateFlow<CardioRun?>(null)
    val run: StateFlow<CardioRun?> = _run.asStateFlow()

    private var job: Job? = null
    // Last fix that counted towards the distance; small moves are measured from it
    private var anchor: LocationPoint? = null

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
            _run.value = CardioRun(
                sessionId, session.type, session.startedAt,
                distanceMeters = RouteDistance.total(saved),
                route = saved,
                tracks = sessionRepo.getTracksForSession(sessionId)
            )
            launch { locationTracker.locationUpdates().collect(::onFix) }
            launch { logTracks(sessionId) }
        }
    }

    /** Stops recording and returns the run as it stood. */
    fun stop(): CardioRun? {
        job?.cancel()
        job = null
        anchor = null
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
        sessionRepo.logLocation(run.sessionId, point.latitude, point.longitude, point.speed, point.altitude, point.timestamp)
        val from = anchor
        val added = if (from == null) 0.0 else RouteDistance.step(from, point)
        if (from == null || added > 0) anchor = point
        _run.update {
            it?.copy(
                distanceMeters = it.distanceMeters + added,
                currentSpeed = point.speed ?: RouteDistance.recentSpeed(it.route, point),
                route = it.route + point
            )
        }
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
        sessionRepo.getLocationsForSession(sessionId).map {
            LocationPoint(it.latitude, it.longitude, it.speed, it.altitude, accuracy = null, timestamp = it.timestamp)
        }

    private companion object {
        const val TAG = "CardioRecorder"
        const val RESUME_WINDOW_MS = 30 * 60_000L
    }
}
