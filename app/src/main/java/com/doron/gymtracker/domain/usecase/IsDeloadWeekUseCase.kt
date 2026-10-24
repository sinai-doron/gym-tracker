package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import javax.inject.Inject

class IsDeloadWeekUseCase @Inject constructor(
    private val prefs: ProgramPrefs
) {
    operator fun invoke(sessionDateMs: Long = System.currentTimeMillis()): Boolean {
        val elapsedMs = sessionDateMs - prefs.programStartEpochMs
        val weekNumber = (elapsedMs / (7L * 24 * 60 * 60 * 1000)).toInt() + 1
        return weekNumber % 6 == 0
    }
}
