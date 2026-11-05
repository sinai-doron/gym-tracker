package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import javax.inject.Inject

/**
 * Per exercise, a rep-range rule: once every set at the top weight reached the top of the range
 * (at least as many sets as planned), add the exercise's step, which comes from its equipment
 * (2.5 kg on a barbell, 2 kg a dumbbell, 5 kg a machine's pin). Stalled after three sessions at
 * the same weight and reps, short of the top.
 */
class ProgressionHintUseCase @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workouts: WorkoutRepository
) {
    suspend operator fun invoke(exercise: RepExerciseDefinition): ProgressionHint {
        // The finished sessions that had this exercise, oldest first, not just the last few
        // sessions overall: with a rotation and runs in between, those rarely include it. Deload
        // weeks are left out: lighter on purpose, they say nothing about progress.
        val program = workouts.getActiveProgram()
        val stats = sessionRepo.getExerciseHistory(exercise.id)
            .filterNot { program?.isDeloadWeek(it.startedAt) == true }
            .mapNotNull { session -> TopSets.of(session.sets.filterIsInstance<RepSetRecord>()) }
        val last = stats.lastOrNull() ?: return ProgressionHint.OnTrack

        // Lighter warm-up sets don't count: the sets at the session's top weight do
        if (last.reps.size >= exercise.targetSets && last.reps.all { it >= exercise.targetReps }) {
            return ProgressionHint.Increase((exercise.weightStepKg ?: DEFAULT_STEP_KG).toFloat())
        }

        val last3 = stats.takeLast(3)
        if (last3.size == 3 && last3.map { it.weight }.distinct().size == 1 && last3.map { it.reps.max() }.distinct().size == 1) {
            return ProgressionHint.Stalled
        }
        return ProgressionHint.OnTrack
    }

    /** A session's top weight and the reps of each set done at it. */
    private data class TopSets(val weight: Float, val reps: List<Int>) {
        companion object {
            fun of(sets: List<RepSetRecord>): TopSets? {
                val top = sets.maxOfOrNull { it.weightKg } ?: return null
                return TopSets(top, sets.filter { it.weightKg == top }.map { it.reps })
            }
        }
    }

    private companion object {
        /** For an exercise whose equipment isn't known: the user's own, or one not from the library. */
        const val DEFAULT_STEP_KG = 2.5
    }
}
