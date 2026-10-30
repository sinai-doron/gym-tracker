package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.RouteFollower
import com.doron.gymtracker.data.RouteNamer
import com.doron.gymtracker.data.db.dao.RouteDao
import com.doron.gymtracker.data.db.entities.RouteEntity
import com.doron.gymtracker.data.db.entities.RoutePointEntity
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.SavedRoute
import javax.inject.Inject
import javax.inject.Singleton

/** Routes to run: saved from past runs or imported from GPX files. */
@Singleton
class RouteRepository @Inject constructor(
    private val dao: RouteDao,
    private val namer: RouteNamer
) {
    /** Every route, newest first, without its points. */
    suspend fun getAll(): List<SavedRoute> = dao.getAll().map { it.toDomain() }

    /** Every route with its points, e.g. to draw them. */
    suspend fun getAllWithPoints(): List<SavedRoute> = dao.getAll().map { r ->
        r.toDomain(dao.getPoints(r.id).map { RoutePoint(it.latitude, it.longitude, it.elevationM) })
    }

    suspend fun get(id: Long): SavedRoute? = dao.get(id)?.toDomain(
        dao.getPoints(id).map { RoutePoint(it.latitude, it.longitude, it.elevationM) }
    )

    /**
     * Saves a route. Without a [name] it's named after where it starts (or "מסלול" if the
     * phone can't tell); it can be renamed later.
     */
    suspend fun save(points: List<RoutePoint>, name: String? = null): Long {
        val title = name?.takeIf { it.isNotBlank() }
            ?: points.firstOrNull()?.let { namer.placeName(it) }
            ?: DEFAULT_NAME
        val simplified = simplify(points)
        return dao.insertWithPoints(
            RouteEntity(name = title, distanceMeters = RouteFollower.length(simplified), createdAt = System.currentTimeMillis())
        ) { id ->
            simplified.mapIndexed { i, p -> RoutePointEntity(id, i, p.latitude, p.longitude, p.elevationM) }
        }
    }

    suspend fun rename(id: Long, name: String) = dao.rename(id, name.trim())

    suspend fun delete(id: Long) = dao.delete(id)

    private fun RouteEntity.toDomain(points: List<RoutePoint> = emptyList()) =
        SavedRoute(id, name, distanceMeters, createdAt, points)

    /** Keeps a point every [MIN_SPACING_M] or so: a 2-second GPS track has far more than following needs. */
    private fun simplify(points: List<RoutePoint>): List<RoutePoint> {
        if (points.size < 3) return points
        val kept = mutableListOf(points.first())
        for (p in points.drop(1).dropLast(1)) {
            if (RouteFollower.meters(kept.last(), p) >= MIN_SPACING_M) kept += p
        }
        kept += points.last()
        return kept
    }

    private companion object {
        const val MIN_SPACING_M = 10.0
        const val DEFAULT_NAME = "מסלול"
    }
}
