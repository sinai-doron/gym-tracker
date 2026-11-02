package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import javax.inject.Inject

class FinishSessionUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository,
    private val healthConnect: HealthConnectManager
) {
    /** [title] is the session's name for Health Connect, resolved by the UI in the user's language. */
    suspend operator fun invoke(sessionId: Long, title: String) {
        sessionRepo.finishSession(sessionId)
        val session = sessionRepo.getLastCompleted() ?: return
        val finishedAt = session.finishedAt ?: return
        if (session.type == "GYM") {
            // The next workout is read from history (GetNextWorkoutUseCase); nothing to record
            workoutRepo.getById(session.workoutId) ?: return
            healthConnect.writeWorkoutSession(session.startedAt, finishedAt, title)
        } else {
            healthConnect.writeWorkoutSession(session.startedAt, finishedAt, title, session.type)
        }
    }
}
