package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.VariantRule
import javax.inject.Inject

data class ProgramStatus(
    /** The name the user gave the programme; null for the seeded plan. */
    val programName: String?,
    val weekNumber: Int,
    /** The programme alternates exercises with their variants, so its weeks are A or B. */
    val hasVariants: Boolean,
    /** A week the programme does exercises' variants (week B). */
    val isWeekB: Boolean,
    val isDeloadWeek: Boolean,
    /** 0 during a deload week; null when the programme has no deloads. */
    val weeksUntilDeload: Int?
)

class GetProgramStatusUseCase @Inject constructor(
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(nowMs: Long = System.currentTimeMillis()): ProgramStatus {
        val program = repo.getActiveProgram()
        return ProgramStatus(
            programName = program.name,
            weekNumber = program.weekNumber(nowMs),
            hasVariants = program.variantRule != VariantRule.NONE,
            isWeekB = program.usesVariants(nowMs),
            isDeloadWeek = program.isDeloadWeek(nowMs),
            weeksUntilDeload = program.weeksUntilDeload(nowMs)
        )
    }
}
