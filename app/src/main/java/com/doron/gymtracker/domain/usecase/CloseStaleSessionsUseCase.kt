package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import javax.inject.Inject

/**
 * Sessions left open (e.g. the app was closed mid-workout) are closed after 24 hours:
 * finished at their last logged set or GPS point, or deleted if nothing was recorded.
 */
class CloseStaleSessionsUseCase @Inject constructor(
    private val sessionRepo: SessionRepository
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()) {
        for (session in sessionRepo.getUnfinishedStartedBefore(now - MAX_SESSION_MS)) {
            val lastActivity = sessionRepo.getLastActivityAt(session.id)
            if (lastActivity == null) {
                sessionRepo.deleteSession(session.id)
            } else {
                sessionRepo.closeSession(session.id, lastActivity.coerceAtMost(session.startedAt + MAX_SESSION_MS))
            }
        }
    }

    companion object {
        const val MAX_SESSION_MS = 24L * 60 * 60 * 1000
    }
}
