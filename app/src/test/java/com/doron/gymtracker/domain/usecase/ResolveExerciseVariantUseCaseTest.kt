package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.prefs.ProgramPrefs
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ResolveExerciseVariantUseCaseTest {

    private lateinit var prefs: ProgramPrefs
    private lateinit var useCase: ResolveExerciseVariantUseCase

    private val startDate = LocalDate.of(2026, 10, 26)
    private val startEpoch = startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    private val exA = RepExerciseDefinition("d1_e3_a", "Week A variant", 3, 90, 10)
    private val exB = RepExerciseDefinition("d1_e3_b", "Week B variant", 3, 90, 10)
    private val exFixed = RepExerciseDefinition("d1_e1", "Fixed exercise", 4, 180, 6)

    @Before
    fun setup() {
        prefs = mockk()
        every { prefs.programStartEpochMs } returns startEpoch
        useCase = ResolveExerciseVariantUseCase(prefs)
    }

    @Test
    fun `week 1 (odd) — returns exercise A`() {
        val week1Date = startEpoch + 0L // day 0 = week 1
        val result = useCase(exA.copy(variantB = exB), week1Date)
        assertEquals("d1_e3_a", result.id)
    }

    @Test
    fun `week 2 (even) — returns variant B`() {
        val week2Date = startEpoch + (7 * 24 * 60 * 60 * 1000L) // 7 days later = week 2
        val result = useCase(exA.copy(variantB = exB), week2Date)
        assertEquals("d1_e3_b", result.id)
    }

    @Test
    fun `week 3 (odd) — returns exercise A again`() {
        val week3Date = startEpoch + (14 * 24 * 60 * 60 * 1000L)
        val result = useCase(exA.copy(variantB = exB), week3Date)
        assertEquals("d1_e3_a", result.id)
    }

    @Test
    fun `fixed exercise (no variantB) — always returns same exercise`() {
        val week2Date = startEpoch + (7 * 24 * 60 * 60 * 1000L)
        val result = useCase(exFixed, week2Date)
        assertEquals("d1_e1", result.id)
    }
}
