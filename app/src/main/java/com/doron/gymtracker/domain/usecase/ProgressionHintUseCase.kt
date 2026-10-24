package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepSetRecord
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ProgressionHintUseCase @Inject constructor(
    private val sessionRepo: SessionRepository
) {
    suspend operator fun invoke(exerciseId: String, targetReps: Int): ProgressionHint {
        val sessions = sessionRepo.observeAll().first()
            .filter { it.finishedAt != null }
            .sortedBy { it.startedAt }

        if (sessions.size < 3) return ProgressionHint.OnTrack

        val last3 = sessions.takeLast(3)

        // Collect per-session: max reps achieved and weight used
        data class SessionStats(val maxReps: Int, val maxWeight: Float)

        val stats = last3.mapNotNull { session ->
            val entries = sessionRepo.getEntriesForSession(session.id)
            val entry = entries.find { it.exerciseId == exerciseId } ?: return@mapNotNull null
            val repSets = entry.sets.filterIsInstance<RepSetRecord>()
            if (repSets.isEmpty()) return@mapNotNull null
            SessionStats(
                maxReps = repSets.maxOf { it.reps },
                maxWeight = repSets.maxOf { it.weightKg }
            )
        }

        if (stats.size < 3) return ProgressionHint.OnTrack

        // Increase: all 3 sessions hit at or above targetReps
        val allHitTarget = stats.all { it.maxReps >= targetReps }
        if (allHitTarget) {
            // Upper body exercises (targetReps ≤ 12) add 2.5kg; lower body (> 12) add 5kg
            val addKg = if (targetReps <= 12) 2.5f else 5f
            return ProgressionHint.Increase(addKg)
        }

        // Stalled: same weight and same reps across all 3 sessions
        val sameWeight = stats.map { it.maxWeight }.distinct().size == 1
        val sameReps = stats.map { it.maxReps }.distinct().size == 1
        if (sameWeight && sameReps) return ProgressionHint.Stalled

        return ProgressionHint.OnTrack
    }
}
