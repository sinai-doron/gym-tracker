package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.SessionDao
import com.doron.gymtracker.data.db.dao.SetRecordDao
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetRecordEntity
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val entryDao: ExerciseEntryDao,
    private val setDao: SetRecordDao
) : SessionRepository {

    override suspend fun startSession(workoutId: String): Long =
        sessionDao.insert(SessionEntity(0, workoutId, System.currentTimeMillis(), null))

    override suspend fun finishSession(sessionId: Long) {
        val existing = sessionDao.getById(sessionId) ?: return
        sessionDao.update(existing.copy(finishedAt = System.currentTimeMillis()))
    }

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
        return setDao.insert(record.toEntity(entry.id))
    }

    override suspend fun updateSetRecord(record: SetRecord) {
        val entryId = record.entryId
        setDao.update(record.toEntity(entryId))
    }

    override suspend fun deleteSetRecord(setId: Long) {
        setDao.deleteById(setId)
    }

    private fun SessionEntity.toDomain() = Session(id, workoutId, startedAt, finishedAt)

    private fun SetRecord.toEntity(entryId: Long) = when (this) {
        is RepSetRecord -> SetRecordEntity(id, entryId, completedAt, "REP", reps, weightKg, null, null)
        is TimedSetRecord -> SetRecordEntity(id, entryId, completedAt, "TIMED", null, null, durationSecs, null)
        is DistanceSetRecord -> SetRecordEntity(id, entryId, completedAt, "DISTANCE", null, null, null, distanceM)
    }

    private fun SetRecordEntity.toDomain(): SetRecord = when (type) {
        "REP" -> RepSetRecord(id, entryId, completedAt, reps!!, weightKg!!)
        "TIMED" -> TimedSetRecord(id, entryId, completedAt, durationSecs!!)
        "DISTANCE" -> DistanceSetRecord(id, entryId, completedAt, distanceM!!)
        else -> TimedSetRecord(id, entryId, completedAt, durationSecs ?: 0)
    }
}
