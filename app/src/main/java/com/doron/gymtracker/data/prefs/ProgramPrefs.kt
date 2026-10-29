package com.doron.gymtracker.data.prefs

import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProgramPrefs @Inject constructor() {
    // Programme start date: 2026-10-26 (hardcoded per ADR-0002)
    val programStartEpochMs: Long =
        LocalDate.of(2026, 10, 26)
            .atStartOfDay()
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()

    /** 1-based programme week containing [atMs]. */
    fun weekNumber(atMs: Long): Int =
        ((atMs - programStartEpochMs) / (7L * 24 * 60 * 60 * 1000)).toInt() + 1
}
