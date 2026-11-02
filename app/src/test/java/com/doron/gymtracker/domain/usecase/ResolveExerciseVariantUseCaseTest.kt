package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.VariantRule
import com.doron.gymtracker.domain.usecase.ProgramFixture.inWeek
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveExerciseVariantUseCaseTest {

    private val repo = ProgramFixture.repo()
    private val useCase = ResolveExerciseVariantUseCase(repo)

    private val exA = RepExerciseDefinition("d1_e3_a", "Week A variant", 3, 90, 10)
    private val exB = RepExerciseDefinition("d1_e3_b", "Week B variant", 3, 90, 10)
    private val exFixed = RepExerciseDefinition("d1_e1", "Fixed exercise", 4, 180, 6)

    @Test
    fun `week 1 (odd) — returns exercise A`() = runTest {
        assertEquals("d1_e3_a", useCase(exA.copy(variantB = exB), ProgramFixture.startMs).id)
    }

    @Test
    fun `week 2 (even) — returns variant B`() = runTest {
        assertEquals("d1_e3_b", useCase(exA.copy(variantB = exB), inWeek(2)).id)
    }

    @Test
    fun `week 3 (odd) — returns exercise A again`() = runTest {
        assertEquals("d1_e3_a", useCase(exA.copy(variantB = exB), inWeek(3)).id)
    }

    @Test
    fun `fixed exercise (no variantB) — always returns same exercise`() = runTest {
        assertEquals("d1_e1", useCase(exFixed, inWeek(2)).id)
    }

    @Test
    fun `a programme without the variant rule never switches`() = runTest {
        coEvery { repo.getActiveProgram() } returns ProgramFixture.program.copy(variantRule = VariantRule.NONE)
        assertEquals("d1_e3_a", useCase(exA.copy(variantB = exB), inWeek(2)).id)
    }
}
