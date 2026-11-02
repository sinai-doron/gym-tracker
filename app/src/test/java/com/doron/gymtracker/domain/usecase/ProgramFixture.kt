package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneOffset

/** The owner's programme (week 1 from 2026-10-26) behind a mocked plan repository. */
object ProgramFixture {
    val program = TrainingPlanData.program(LocalDate.of(2026, 10, 26))
    val startMs: Long = program.startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000

    /** A moment a second into week [n]. */
    fun inWeek(n: Int) = startMs + (n - 1) * WEEK_MS + 1_000

    fun repo(): WorkoutRepository = mockk<WorkoutRepository>().also { coEvery { it.getActiveProgram() } returns program }
}
