package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.SetRecord
import javax.inject.Inject

class LogSetUseCase @Inject constructor(
    private val repo: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, exerciseId: String, record: SetRecord): Long =
        repo.addSetRecord(sessionId, exerciseId, record)
}
