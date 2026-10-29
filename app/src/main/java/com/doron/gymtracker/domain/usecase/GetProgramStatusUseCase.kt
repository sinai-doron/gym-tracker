package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import com.doron.gymtracker.domain.usecase.IsDeloadWeekUseCase.Companion.DELOAD_EVERY_WEEKS
import javax.inject.Inject

data class ProgramStatus(
    val weekNumber: Int,
    /** Even weeks use the week-B exercise variants. */
    val isWeekB: Boolean,
    val isDeloadWeek: Boolean,
    /** 0 during a deload week. */
    val weeksUntilDeload: Int
)

class GetProgramStatusUseCase @Inject constructor(
    private val prefs: ProgramPrefs
) {
    operator fun invoke(nowMs: Long = System.currentTimeMillis()): ProgramStatus {
        val week = prefs.weekNumber(nowMs)
        val untilDeload = (DELOAD_EVERY_WEEKS - week % DELOAD_EVERY_WEEKS) % DELOAD_EVERY_WEEKS
        return ProgramStatus(
            weekNumber = week,
            isWeekB = week % 2 == 0,
            isDeloadWeek = untilDeload == 0,
            weeksUntilDeload = untilDeload
        )
    }
}
