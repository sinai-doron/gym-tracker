package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import com.doron.gymtracker.domain.model.ExerciseDefinition
import javax.inject.Inject

class ResolveExerciseVariantUseCase @Inject constructor(
    private val prefs: ProgramPrefs
) {
    operator fun invoke(
        exercise: ExerciseDefinition,
        sessionDateMs: Long = System.currentTimeMillis()
    ): ExerciseDefinition {
        val variantB = exercise.variantB ?: return exercise
        val elapsedMs = sessionDateMs - prefs.programStartEpochMs
        val weekNumber = (elapsedMs / (7L * 24 * 60 * 60 * 1000)).toInt() + 1
        return if (weekNumber % 2 == 0) variantB else exercise
    }
}
