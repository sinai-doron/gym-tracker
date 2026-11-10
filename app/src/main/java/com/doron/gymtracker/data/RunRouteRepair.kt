package com.doron.gymtracker.data

import android.util.Log
import com.doron.gymtracker.data.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

/** A run or walk as this app wrote it to Health Connect: matched to its session by start time. */
data class RecordedRoute(val startedAt: Long, val points: List<LocationPoint>)

/**
 * Puts back GPS points a sync took from finished runs. Until November 2026 a download could
 * replace a run's route with the few points uploaded as it began. Every finished run was also
 * written to Health Connect with its whole route, so the route is restored from there and, being
 * a local change, uploaded again on the next sync.
 */
@Singleton
class RunRouteRepair @Inject constructor(
    private val healthConnect: HealthConnectManager,
    private val sessionRepo: SessionRepository
) {
    suspend fun repair(now: Long = System.currentTimeMillis()) {
        val since = now - LOOK_BACK_MS
        val runs = sessionRepo.getCompletedSince(since).filter { it.type == "RUNNING" || it.type == "WALKING" }
        if (runs.isEmpty()) return
        val recorded = healthConnect.readOwnRunRoutes(since) ?: return
        val local = runs.associate { it.startedAt to (it.id to sessionRepo.getRouteForSession(it.id).size) }
        restorable(local, recorded).forEach { (sessionId, points) ->
            Log.i(TAG, "Restoring ${points.size} GPS points to session $sessionId from Health Connect")
            sessionRepo.replaceRoute(sessionId, points)
        }
    }

    companion object {
        private const val TAG = "RunRouteRepair"
        // The bug came with sync, on 2026-10-31; a year covers it with room to spare
        private const val LOOK_BACK_MS = 365L * 24 * 60 * 60 * 1000

        /**
         * The routes to put back, by session id: where Health Connect holds more points for a
         * run than the phone does. [local] is each run's start time to its id and point count.
         */
        fun restorable(local: Map<Long, Pair<Long, Int>>, recorded: List<RecordedRoute>): Map<Long, List<LocationPoint>> =
            recorded.mapNotNull { route ->
                val (sessionId, count) = local[route.startedAt] ?: return@mapNotNull null
                if (route.points.size > count) sessionId to route.points else null
            }.toMap()
    }
}
