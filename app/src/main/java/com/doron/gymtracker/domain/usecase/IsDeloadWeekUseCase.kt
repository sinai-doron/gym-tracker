package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import javax.inject.Inject

class IsDeloadWeekUseCase @Inject constructor(
    private val repo: WorkoutRepository
) {
    suspend operator fun invoke(sessionDateMs: Long = System.currentTimeMillis()): Boolean =
        repo.getActiveProgram().isDeloadWeek(sessionDateMs)
}
