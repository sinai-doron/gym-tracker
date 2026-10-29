package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.RunDetails
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TrackLog
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun startSession(workoutId: String): Long
    suspend fun startCardioSession(type: String): Long
    suspend fun finishSession(sessionId: Long)
    suspend fun finishCardioSession(sessionId: Long, distanceMeters: Double, finishedAt: Long = System.currentTimeMillis())
    fun observeAll(): Flow<List<Session>>
    suspend fun getById(sessionId: Long): Session?
    suspend fun getLastCompleted(): Session?
    suspend fun getEntriesForSession(sessionId: Long): List<ExerciseEntry>
    suspend fun addSetRecord(sessionId: Long, exerciseId: String, record: SetRecord): Long
    suspend fun updateSetRecord(record: SetRecord)
    suspend fun deleteSetRecord(setId: Long)
    suspend fun logTrack(sessionId: Long, title: String, artist: String)
    suspend fun getTracksForSession(sessionId: Long): List<TrackLog>
    suspend fun logLocation(sessionId: Long, point: LocationPoint)
    suspend fun getRouteForSession(sessionId: Long): List<LocationPoint>
    suspend fun getRunDetails(sessionId: Long): RunDetails?
    suspend fun saveRunDetails(details: RunDetails)
    suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: String)
    suspend fun deleteSession(sessionId: Long)
    suspend fun getUnfinishedStartedBefore(beforeMillis: Long): List<Session>
    /** The gym session still in progress, if any (e.g. the app was closed mid-workout). */
    suspend fun getOpenGymSession(): Session?
    /** A run or walk that was started and never finished, if any. */
    suspend fun getOpenCardioSession(): Session?
    /** The exercise's sets per finished session, oldest session first. */
    suspend fun getExerciseHistory(exerciseId: String): List<ExerciseSessionSets>
    /** Time of the last logged set or GPS point, or null if nothing was recorded. */
    suspend fun getLastActivityAt(sessionId: Long): Long?
    suspend fun closeSession(sessionId: Long, finishedAt: Long)
    suspend fun getCompletedSince(sinceMillis: Long): List<Session>
    suspend fun getTotalVolumeSince(sinceMillis: Long): Float
}
