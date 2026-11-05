package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import javax.inject.Inject

class GetLastWeightUseCase @Inject constructor(
    private val repo: SessionRepository,
    private val workouts: WorkoutRepository
) {
    // The previous session is usually a different workout (A/B/C rotation),
    // so look back for the last session that included this exercise. A deload week's lighter
    // weights are passed over: the week after one starts from the usual weight.
    suspend operator fun invoke(exerciseId: String): Float? {
        val program = workouts.getActiveProgram()
        for (session in repo.getCompletedSince(0).take(MAX_SESSIONS_BACK)) {
            if (session.type != "GYM") continue
            if (program?.isDeloadWeek(session.startedAt) == true) continue
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
