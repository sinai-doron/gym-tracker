package com.doron.gymtracker.data.repository

import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.SetRecord
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun startSession(workoutId: String): Long
    suspend fun finishSession(sessionId: Long)
    fun observeAll(): Flow<List<Session>>
    suspend fun getLastCompleted(): Session?
    suspend fun getEntriesForSession(sessionId: Long): List<ExerciseEntry>
    suspend fun addSetRecord(sessionId: Long, exerciseId: String, record: SetRecord): Long
}
