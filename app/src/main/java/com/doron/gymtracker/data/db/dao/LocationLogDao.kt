package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.LocationLogEntity

/** How many GPS points one session has. */
data class SessionPointCount(val sessionId: Long, val points: Int)

/** The box a session's GPS points fall in, and how many there are. */
data class TrackExtent(
    val sessionId: Long,
    val points: Int,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)

@Dao
interface LocationLogDao {
    @Insert
    suspend fun insert(entity: LocationLogEntity): Long

    @Query("SELECT * FROM location_log WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getForSession(sessionId: Long): List<LocationLogEntity>

    @Query("DELETE FROM location_log WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("SELECT sessionId, count(*) AS points FROM location_log GROUP BY sessionId")
    suspend fun countBySession(): List<SessionPointCount>

    @Query(
        "SELECT sessionId, count(*) AS points, min(latitude) AS minLat, max(latitude) AS maxLat, " +
            "min(longitude) AS minLon, max(longitude) AS maxLon FROM location_log GROUP BY sessionId"
    )
    suspend fun extents(): List<TrackExtent>

    /** The session's point at [offset] in time order: its middle one, to say where it was. */
    @Query("SELECT * FROM location_log WHERE sessionId = :sessionId ORDER BY timestamp LIMIT 1 OFFSET :offset")
    suspend fun pointAt(sessionId: Long, offset: Int): LocationLogEntity?

    @Insert
    suspend fun insertAll(entities: List<LocationLogEntity>)

    /** A session's points swapped for [entities] in one go: never half a route. */
    @Transaction
    suspend fun replaceForSession(sessionId: Long, entities: List<LocationLogEntity>) {
        deleteForSession(sessionId)
        insertAll(entities)
    }
}
