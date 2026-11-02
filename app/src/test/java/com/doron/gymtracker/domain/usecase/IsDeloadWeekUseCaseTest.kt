package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.domain.usecase.ProgramFixture.inWeek
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IsDeloadWeekUseCaseTest {

    private val useCase = IsDeloadWeekUseCase(ProgramFixture.repo())

    @Test
    fun `week 1 is not deload`() = runTest { assertFalse(useCase(ProgramFixture.startMs)) }

    @Test
    fun `week 5 is not deload`() = runTest { assertFalse(useCase(inWeek(5))) }

    @Test
    fun `week 6 is deload`() = runTest { assertTrue(useCase(inWeek(6))) }

    @Test
    fun `week 7 is not deload`() = runTest { assertFalse(useCase(inWeek(7))) }

    @Test
    fun `week 12 is deload`() = runTest { assertTrue(useCase(inWeek(12))) }
}
