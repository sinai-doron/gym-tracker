package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.Pause
import com.doron.gymtracker.data.StepMark
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.LocationLogDao
import com.doron.gymtracker.data.db.dao.RunDetailsDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetDao
import com.doron.gymtracker.data.db.dao.SetWithParts
import com.doron.gymtracker.data.db.dao.TrackLogDao
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.data.db.entities.RunDetailsEntity
import com.doron.gymtracker.data.db.entities.RunPauseEntity
import com.doron.gymtracker.data.db.entities.RunStepEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetPartEntity
import com.doron.gymtracker.data.db.entities.TrackLogEntity
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.ExerciseSessionSets
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.RunDetails
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.model.WeatherCondition
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val entryDao: ExerciseEntryDao,
    private val setDao: SetDao,
    private val trackLogDao: TrackLogDao,
    private val locationLogDao: LocationLogDao,
    private val runDetailsDao: RunDetailsDao
) : SessionRepository {

    override suspend fun startSession(workoutId: String): Long =
        sessionDao.insert(SessionEntity(0, workoutId, System.currentTimeMillis(), null))

    override suspend fun startCardioSession(type: String): Long =
        sessionDao.insert(SessionEntity(0, type, System.currentTimeMillis(), null, type = type))

    override suspend fun startTimedSession(activityTypeId: String): Long = sessionDao.insert(
        SessionEntity(0, TIMED, System.currentTimeMillis(), null, type = TIMED, activityTypeId = activityTypeId)
    )

    override suspend fun finishTimedSession(sessionId: Long, finishedAt: Long, calories: Int?, note: String?) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(
            existing.copy(
                finishedAt = finishedAt,
                durationS = ((finishedAt - existing.startedAt) / 1000).toInt(),
                calories = calories,
                note = note?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    override suspend fun logSession(
        type: String,
        workoutId: String,
        activityTypeId: String,
        startedAt: Long,
        durationS: Int,
        distanceMeters: Double?,
        calories: Int?,
        note: String?
    ): Long = sessionDao.insert(
        SessionEntity(
            id = 0,
            workoutId = workoutId,
            startedAt = startedAt,
            finishedAt = startedAt + durationS * 1000L,
            type = type,
            distanceMeters = distanceMeters,
            activityTypeId = activityTypeId,
            loggedManually = true,
            durationS = durationS,
            calories = calories,
            note = note?.trim()?.takeIf { it.isNotEmpty() }
        )
    )

    override suspend fun getOpenTimedSession(): Session? = sessionDao.getLatestUnfinishedTimed()?.toDomain()

    override suspend fun finishSession(sessionId: Long) {
        val existing = sessionDao.getById(sessionId) ?: return
        // One logged by hand already ends when the user said it did
        if (existing.loggedManually && existing.finishedAt != null) return
        sessionDao.update(existing.copy(finishedAt = System.currentTimeMillis()))
    }

    override suspend fun finishCardioSession(sessionId: Long, distanceMeters: Double, finishedAt: Long) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(existing.copy(finishedAt = finishedAt, distanceMeters = distanceMeters))
    }

    override suspend fun getById(sessionId: Long): Session? =
        sessionDao.getById(sessionId)?.toDomain()

    override fun observeAll(): Flow<List<Session>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getLastCompleted(): Session? =
        sessionDao.getLastCompleted()?.toDomain()

    override suspend fun getEntriesForSession(sessionId: Long): List<ExerciseEntry> {
        val entries = entryDao.getForSession(sessionId)
        return entries.map { entry ->
            val sets = setDao.getForEntry(entry.id).map { it.toDomain() }
            ExerciseEntry(entry.id, entry.sessionId, entry.exerciseId, sets)
        }
    }

    override suspend fun addSetRecord(sessionId: Long, exerciseId: String, record: SetRecord): Long {
        val entry = entryDao.getForSessionAndExercise(sessionId, exerciseId)
            ?: run {
                val newId = entryDao.insert(ExerciseEntryEntity(0, sessionId, exerciseId))
                ExerciseEntryEntity(newId, sessionId, exerciseId)
            }
        return setDao.insert(entry.id, completedAt(sessionId, record.completedAt), record.toParts())
    }

    /** When a set counts as done: now, or for a session logged by hand, within its time. */
    private suspend fun completedAt(sessionId: Long, at: Long): Long {
        val session = sessionDao.getById(sessionId)
        val end = session?.finishedAt
        return if (session?.loggedManually == true && end != null) at.coerceIn(session.startedAt, end) else at
    }

    override suspend fun updateSetRecord(record: SetRecord) {
        setDao.update(record.id, record.completedAt, record.toParts())
    }

    override suspend fun deleteSetRecord(setId: Long) {
        setDao.deleteById(setId)
    }

    override suspend fun logTrack(sessionId: Long, title: String, artist: String) {
        trackLogDao.insert(TrackLogEntity(0, sessionId, System.currentTimeMillis(), title, artist))
    }

    override suspend fun getTracksForSession(sessionId: Long): List<TrackLog> =
        trackLogDao.getForSession(sessionId).map {
            TrackLog(it.id, it.sessionId, it.startedAt, it.title, it.artist)
        }

    override suspend fun logLocation(sessionId: Long, point: LocationPoint) {
        locationLogDao.insert(
            LocationLogEntity(
                0, sessionId, point.timestamp, point.latitude, point.longitude,
                point.speed, point.altitude, point.accuracy, point.bearing
            )
        )
    }

    override suspend fun getRouteForSession(sessionId: Long): List<LocationPoint> =
        locationLogDao.getForSession(sessionId).map {
            LocationPoint(it.latitude, it.longitude, it.speed, it.altitude, it.accuracy, it.timestamp, it.bearing)
        }

    override suspend fun getRunDetails(sessionId: Long): RunDetails? =
        runDetailsDao.get(sessionId)?.let {
            val type = runCatching { RunTargetType.valueOf(it.targetType) }.getOrDefault(RunTargetType.NONE)
            RunDetails(
                sessionId = it.sessionId,
                target = when (type) {
                    RunTargetType.DISTANCE -> RunTarget(type, distanceKm = it.targetValue?.toFloat() ?: RunTarget.DEFAULT_DISTANCE_KM)
                    RunTargetType.DURATION -> RunTarget(type, durationMin = it.targetValue?.toInt() ?: RunTarget.DEFAULT_DURATION_MIN)
                    RunTargetType.ROUTE -> RunTarget(type, routeId = it.targetValue?.toLong())
                    RunTargetType.NONE -> RunTarget(type)
                },
                lapMeters = it.lapMeters,
                stepCounterAtStart = it.stepCounterAtStart,
                steps = it.steps,
                elevationGainM = it.elevationGainM,
                calories = it.calories,
                weatherTempC = it.weatherTempC,
                weatherCondition = it.weatherCondition?.let { c -> runCatching { WeatherCondition.valueOf(c) }.getOrNull() }
            )
        }

    override suspend fun saveRunDetails(details: RunDetails) {
        val target = details.target
        runDetailsDao.upsert(
            RunDetailsEntity(
                sessionId = details.sessionId,
                targetType = target.type.name,
                targetValue = when (target.type) {
                    RunTargetType.DISTANCE -> target.distanceKm.toDouble()
                    RunTargetType.DURATION -> target.durationMin.toDouble()
                    RunTargetType.ROUTE -> target.routeId?.toDouble()
                    RunTargetType.NONE -> null
                },
                lapMeters = details.lapMeters,
                stepCounterAtStart = details.stepCounterAtStart,
                steps = details.steps,
                elevationGainM = details.elevationGainM,
                calories = details.calories,
                weatherTempC = details.weatherTempC,
                weatherCondition = details.weatherCondition?.name
            )
        )
    }

    override suspend fun deleteExerciseFromSession(sessionId: Long, exerciseId: String) {
        setDao.deleteForExercise(sessionId, exerciseId)
        entryDao.deleteForSessionAndExercise(sessionId, exerciseId)
    }

    override suspend fun getUnfinishedStartedBefore(beforeMillis: Long): List<Session> =
        sessionDao.getUnfinishedStartedBefore(beforeMillis).map { it.toDomain() }

    override suspend fun getOpenGymSession(): Session? =
        sessionDao.getLatestUnfinishedGym()?.toDomain()

    override suspend fun getOpenCardioSession(): Session? =
        sessionDao.getLatestUnfinishedCardio()?.toDomain()

    override suspend fun getExerciseHistory(exerciseId: String): List<ExerciseSessionSets> =
        setDao.getHistoryForExercise(exerciseId)
            .groupBy { it.sessionId }
            .map { (sessionId, rows) ->
                ExerciseSessionSets(sessionId, rows.first().sessionStartedAt, rows.map { SetWithParts(it.set, it.parts).toDomain() })
            }

    override suspend fun getLastActivityAt(sessionId: Long): Long? {
        val lastSet = setDao.getForSession(sessionId).maxOfOrNull { it.set.completedAt }
        val lastLocation = locationLogDao.getForSession(sessionId).lastOrNull()?.timestamp
        return listOfNotNull(lastSet, lastLocation).maxOrNull()
    }

    override suspend fun closeSession(sessionId: Long, finishedAt: Long) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(existing.copy(finishedAt = finishedAt))
    }

    override suspend fun deleteSession(sessionId: Long) {
        setDao.deleteForSession(sessionId)
        entryDao.deleteForSession(sessionId)
        trackLogDao.deleteForSession(sessionId)
        locationLogDao.deleteForSession(sessionId)
        runDetailsDao.deleteForSession(sessionId)
        runDetailsDao.deletePauses(sessionId)
        runDetailsDao.deleteSteps(sessionId)
        sessionDao.deleteById(sessionId)
    }

    override suspend fun getPauses(sessionId: Long): List<Pause> =
        runDetailsDao.getPauses(sessionId).map { Pause(it.startedAt, it.endedAt) }

    override suspend fun savePause(sessionId: Long, pause: Pause) =
        runDetailsDao.upsertPause(RunPauseEntity(sessionId, pause.startedAt, pause.endedAt))

    override suspend fun getSteps(sessionId: Long): List<StepMark> =
        runDetailsDao.getSteps(sessionId).map { StepMark(it.timestamp, it.steps) }

    override suspend fun saveSteps(sessionId: Long, mark: StepMark) =
        runDetailsDao.upsertSteps(RunStepEntity(sessionId, mark.timestamp, mark.steps))

    override suspend fun getCompletedSince(sinceMillis: Long): List<Session> =
        sessionDao.getCompletedSince(sinceMillis).map { it.toDomain() }

    override suspend fun getTotalVolumeSince(sinceMillis: Long): Float {
        val sessions = sessionDao.getCompletedSince(sinceMillis)
        var total = 0f
        for (session in sessions) {
            if (session.type != "GYM") continue
            val sets = setDao.getForSession(session.id)
            for (part in sets.flatMap { it.parts }) {
                if (part.reps != null && part.loadKg != null) total += part.reps * part.loadKg
            }
        }
        return total
    }

    private fun SessionEntity.toDomain() =
        Session(id, workoutId, startedAt, finishedAt, type, distanceMeters, activityTypeId, loggedManually, durationS, calories, note)

    // A set the app logs today has one part (ADR-0006). 0 kg on screen means no load, stored as null.
    private fun SetRecord.toParts(): List<SetPartEntity> = listOf(
        when (this) {
            is RepSetRecord -> SetPartEntity(0, 1, reps = reps, loadKg = weightKg.takeIf { it > 0f })
            is TimedSetRecord -> SetPartEntity(0, 1, durationS = durationSecs)
            is DistanceSetRecord -> SetPartEntity(0, 1, distanceM = distanceM)
        }
    )

    // Until the logging screens show parts, a set reads as its first part.
    private fun SetWithParts.toDomain(): SetRecord {
        val part = parts.minByOrNull { it.seq }
        return when {
            part?.reps != null -> RepSetRecord(set.id, set.entryId, set.completedAt, part.reps, part.loadKg ?: 0f)
            part?.distanceM != null -> DistanceSetRecord(set.id, set.entryId, set.completedAt, part.distanceM)
            else -> TimedSetRecord(set.id, set.entryId, set.completedAt, part?.durationS ?: 0)
        }
    }

    private companion object {
        /** A timed session's type, and its workoutId: it names no workout. */
        const val TIMED = "OTHER"
    }
}
