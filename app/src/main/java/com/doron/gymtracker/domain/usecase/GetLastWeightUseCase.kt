package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import javax.inject.Inject

class GetLastWeightUseCase @Inject constructor(
    private val repo: SessionRepository
) {
    // The previous session is usually a different workout (A/B/C rotation),
    // so look back for the last session that included this exercise.
    suspend operator fun invoke(exerciseId: String): Float? {
        for (session in repo.getCompletedSince(0).take(MAX_SESSIONS_BACK)) {
            if (session.type != "GYM") continue
            val entry = repo.getEntriesForSession(session.id).find { it.exerciseId == exerciseId } ?: continue
            val maxWeight = entry.sets.filterIsInstance<RepSetRecord>().maxOfOrNull { it.weightKg }
            if (maxWeight != null) return maxWeight
        }
        return null
    }

    private companion object {
        const val MAX_SESSIONS_BACK = 30
    }
}
