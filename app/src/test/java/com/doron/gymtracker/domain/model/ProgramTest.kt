package com.doron.gymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class ProgramTest {

    private val start = LocalDate.of(2026, 11, 2)
    private val startMs = start.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

    /** A moment a second into week [n]. */
    private fun inWeek(n: Int) = startMs + (n - 1) * 7L * 24 * 60 * 60 * 1000 + 1_000

    private fun program(deloadEvery: Int? = null, variantEvery: Int = 1, rule: VariantRule = VariantRule.ALTERNATE_WEEKLY) =
        Program("p", null, start, deloadEvery, rule, variantEveryWeeks = variantEvery)

    /** The weeks 1 to 8 that do exercises' variants. */
    private fun variantWeeks(program: Program) = (1..8).filter { program.usesVariants(inWeek(it)) }

    @Test
    fun `variants every other week by default`() {
        assertEquals(listOf(2, 4, 6, 8), variantWeeks(program()))
    }

    @Test
    fun `variants every two weeks`() {
        assertEquals(listOf(3, 4, 7, 8), variantWeeks(program(variantEvery = 2)))
    }

    @Test
    fun `no variants when the rule is off`() {
        assertEquals(emptyList<Int>(), variantWeeks(program(variantEvery = 2, rule = VariantRule.NONE)))
    }

    @Test
    fun `a deload every Nth week, never before the programme starts`() {
        val p = program(deloadEvery = 4)
        assertEquals(listOf(4, 8), (1..8).filter { p.isDeloadWeek(inWeek(it)) })
        // Week 0 and before would divide evenly; they are no deload
        assertFalse(p.isDeloadWeek(inWeek(0)))
        assertFalse(p.isDeloadWeek(inWeek(-3)))
    }

    @Test
    fun `a deload's sets never exceed the plan's`() {
        val p = program(deloadEvery = 4).copy(deloadSets = 2)
        assertEquals(2, p.deloadSetsFor(4))
        assertEquals(1, p.deloadSetsFor(1))
    }
}
