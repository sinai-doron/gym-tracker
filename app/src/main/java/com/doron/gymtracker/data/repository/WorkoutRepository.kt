package com.doron.gymtracker.data.repository

import com.doron.gymtracker.domain.model.Workout

interface WorkoutRepository {
    suspend fun ensureSeeded()
    suspend fun getAll(): List<Workout>
    suspend fun getById(id: String): Workout?
    suspend fun updateTargetSets(exerciseId: String, sets: Int)
}
