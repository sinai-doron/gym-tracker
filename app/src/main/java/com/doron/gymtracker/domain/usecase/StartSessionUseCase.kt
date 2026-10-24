package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import javax.inject.Inject

class StartSessionUseCase @Inject constructor(
    private val repo: SessionRepository
) {
    suspend operator fun invoke(workoutId: String): Long = repo.startSession(workoutId)
}
