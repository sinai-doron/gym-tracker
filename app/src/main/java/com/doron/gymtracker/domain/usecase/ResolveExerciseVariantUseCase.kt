package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ExerciseDefinition
import javax.inject.Inject

/** The exercise or its variant, as the active programme's variant rule picks for the week of [sessionDateMs]. */
class ResolveExerciseVariantUseCase @Inject constructor(
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(
        exercise: ExerciseDefinition,
        sessionDateMs: Long = System.currentTimeMillis()
    ): ExerciseDefinition {
        val variantB = exercise.variantB ?: return exercise
        return if (repo.getActiveProgram()?.usesVariants(sessionDateMs) == true) variantB else exercise
    }
}
