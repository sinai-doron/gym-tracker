package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Program
import javax.inject.Inject

class IsDeloadWeekUseCase @Inject constructor(
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(sessionDateMs: Long = System.currentTimeMillis()): Boolean = program(sessionDateMs) != null

    /** The active programme when [sessionDateMs] falls in one of its deload weeks, for its load and sets; else null. */
    suspend fun program(sessionDateMs: Long): Program? = repo.getActiveProgram()?.takeIf { it.isDeloadWeek(sessionDateMs) }
}
