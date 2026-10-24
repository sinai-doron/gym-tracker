package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import javax.inject.Inject

class GetLastWeightUseCase @Inject constructor(
    private val repo: SessionRepository
) {
    suspend operator fun invoke(exerciseId: String): Float? {
        val lastSession = repo.getLastCompleted() ?: return null
        val entries = repo.getEntriesForSession(lastSession.id)
        val entry = entries.find { it.exerciseId == exerciseId } ?: return null
        return entry.sets
            .filterIsInstance<RepSetRecord>()
            .maxOfOrNull { it.weightKg }
    }
}
