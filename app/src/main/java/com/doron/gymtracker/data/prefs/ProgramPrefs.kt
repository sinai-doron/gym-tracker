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
}
