package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TrackLog
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun startSession(workoutId: String): Long
    suspend fun startCardioSession(type: String): Long
    suspend fun finishSession(sessionId: Long)
    suspend fun finishCardioSession(sessionId: Long, distanceMeters: Double)
    fun observeAll(): Flow<List<Session>>
    suspend fun getById(sessionId: Long): Session?
    suspend fun getLastCompleted(): Session?
    suspend fun getEntriesForSession(sessionId: Long): List<ExerciseEntry>
    suspend fun addSetRecord(sessionId: Long, exerciseId: String, record: SetRecord): Long
    suspend fun updateSetRecord(record: SetRecord)
    suspend fun deleteSetRecord(setId: Long)
    suspend fun logTrack(sessionId: Long, title: String, artist: String)
    suspend fun getTracksForSession(sessionId: Long): List<TrackLog>
    suspend fun logLocation(sessionId: Long, lat: Double, lng: Double, speed: Float?, altitude: Double?)
    suspend fun getLocationsForSession(sessionId: Long): List<LocationLogEntity>
    suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: String)
    suspend fun deleteSession(sessionId: Long)
    suspend fun getCompletedSince(sinceMillis: Long): List<Session>
    suspend fun getTotalVolumeSince(sinceMillis: Long): Float
}
