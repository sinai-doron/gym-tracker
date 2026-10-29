package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import javax.inject.Inject

class IsDeloadWeekUseCase @Inject constructor(
    private val prefs: ProgramPrefs
) {
    operator fun invoke(sessionDateMs: Long = System.currentTimeMillis()): Boolean {
        return prefs.weekNumber(sessionDateMs) % DELOAD_EVERY_WEEKS == 0
    }

    companion object {
        const val DELOAD_EVERY_WEEKS = 6
    }
}
