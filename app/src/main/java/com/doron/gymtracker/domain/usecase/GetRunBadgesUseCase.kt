package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.BadgeStatus
import com.doron.gymtracker.domain.model.RunBadge
import javax.inject.Inject

/**
 * Which distance badges have been earned, worked out from the finished runs (walks don't
 * count), so runs from before badges existed count too.
 */
class GetRunBadgesUseCase @Inject constructor(
    private val sessionRepo: SessionRepository
) {
    suspend operator fun invoke(): List<BadgeStatus> {
        val runs = sessionRepo.getCompletedSince(0)
            .filter { it.type == "RUNNING" && it.distanceMeters != null }
            .sortedBy { it.startedAt }
        return RunBadge.entries.map { badge ->
            val first = runs.firstOrNull { it.distanceMeters!! >= badge.meters }
            BadgeStatus(badge, first?.startedAt, first?.id)
        }
    }

    /** The badges [sessionId] earned for the first time. */
    suspend fun earnedBy(sessionId: Long): List<RunBadge> =
        invoke().filter { it.sessionId == sessionId }.map { it.badge }
}
