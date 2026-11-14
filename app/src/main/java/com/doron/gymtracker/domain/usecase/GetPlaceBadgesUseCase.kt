package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.db.dao.TrackExtent
import com.doron.gymtracker.data.places.Place
import com.doron.gymtracker.data.places.PlaceAtlas
import com.doron.gymtracker.data.repository.SessionRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.hypot

/** A place's badge and the run that first earned it, if any has. */
data class PlaceBadgeStatus(val place: Place, val earnedAt: Long? = null, val sessionId: Long? = null) {
    val isEarned: Boolean get() = sessionId != null
}

/** Each run's places, kept while the app runs; redone when its GPS points change. */
@Singleton
class RunPlacesCache @Inject constructor() {
    private val places = ConcurrentHashMap<Long, Pair<Int, List<String>>>()

    suspend fun get(sessionId: Long, points: Int, work: suspend () -> List<String>): List<String> {
        places[sessionId]?.takeIf { it.first == points }?.let { return it.second }
        return work().also { places[sessionId] = points to it }
    }
}

/**
 * The place badges: a run earns its country's, and its state's in the US, the first time it is
 * there. Runs from before badges existed count, imported ones too. A run whose GPS didn't move
 * doesn't: during the war phones were thrown to Beirut's or Cairo's airport, every point on one spot.
 */
class GetPlaceBadgesUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val atlas: PlaceAtlas,
    private val cache: RunPlacesCache
) {
    suspend operator fun invoke(): List<PlaceBadgeStatus> {
        val places = atlas.all()
        val extents = sessionRepo.trackExtents()
        val runs = sessionRepo.getCompletedSince(0)
            .filter { it.type == "RUNNING" && it.finishedAt != null && !it.loggedManually }
            .sortedBy { it.startedAt }
        val first = mutableMapOf<String, Pair<Long, Long>>() // place -> (startedAt, session)
        for (run in runs) {
            val extent = extents[run.id] ?: continue
            val ids = cache.get(run.id, extent.points) { placesOf(run.id, extent, places) }
            ids.forEach { first.putIfAbsent(it, run.startedAt to run.id) }
        }
        return places.map { place -> first[place.id].let { PlaceBadgeStatus(place, it?.first, it?.second) } }
    }

    /** The places [sessionId] earned for the first time. */
    suspend fun earnedBy(sessionId: Long): List<Place> =
        invoke().filter { it.sessionId == sessionId }.map { it.place }

    private suspend fun placesOf(sessionId: Long, extent: TrackExtent, places: List<Place>): List<String> {
        if (!moved(extent)) return emptyList()
        val (lat, lon) = sessionRepo.middlePoint(sessionId, extent.points) ?: return emptyList()
        return PlaceAtlas.locate(places, lat, lon).map { it.id }
    }

    companion object {
        /** Less ground than this, corner to corner, is GPS stuck in one spot, not a run. */
        const val MIN_SPAN_M = 200.0

        fun moved(extent: TrackExtent): Boolean {
            val midLat = Math.toRadians((extent.minLat + extent.maxLat) / 2)
            val northSouth = (extent.maxLat - extent.minLat) * 111_320
            val eastWest = (extent.maxLon - extent.minLon) * 111_320 * cos(midLat)
            return hypot(northSouth, eastWest) >= MIN_SPAN_M
        }
    }
}
