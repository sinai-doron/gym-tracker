package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import javax.inject.Inject

data class ProgramStatus(
    val weekNumber: Int,
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
            weekNumber = program.weekNumber(nowMs),
            isWeekB = program.usesVariants(nowMs),
            isDeloadWeek = program.isDeloadWeek(nowMs),
            weeksUntilDeload = program.weeksUntilDeload(nowMs)
        )
    }
}
