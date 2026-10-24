package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.RotationPrefs
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.WorkoutLabel
import javax.inject.Inject

class GetNextWorkoutUseCase @Inject constructor(
    private val prefs: RotationPrefs,
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(): Workout {
        val last = prefs.getLastLabel()
        val nextLabel = when (last) {
            null -> WorkoutLabel.A
            WorkoutLabel.A -> WorkoutLabel.B
            WorkoutLabel.B -> WorkoutLabel.C
            WorkoutLabel.C -> WorkoutLabel.A
        }
        return repo.getAll().first { it.label == nextLabel }
    }
}
