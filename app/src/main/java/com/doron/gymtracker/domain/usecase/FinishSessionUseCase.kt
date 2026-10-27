package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.RotationPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import javax.inject.Inject

class FinishSessionUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository,
    private val prefs: RotationPrefs,
    private val healthConnect: HealthConnectManager
) {
    suspend operator fun invoke(sessionId: Long) {
        sessionRepo.finishSession(sessionId)
        val session = sessionRepo.getLastCompleted() ?: return
        val finishedAt = session.finishedAt ?: return
        if (session.type == "GYM") {
            val workout = workoutRepo.getById(session.workoutId) ?: return
            prefs.setLastLabel(workout.label)
            healthConnect.writeWorkoutSession(session.startedAt, finishedAt, workout.label.displayName)
        } else {
            val title = if (session.type == "RUNNING") "ריצה" else "הליכה"
            healthConnect.writeWorkoutSession(session.startedAt, finishedAt, title, session.type)
        }
    }
}
