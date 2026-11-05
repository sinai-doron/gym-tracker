package com.doron.gymtracker.domain.model

import java.time.LocalDate
import java.time.ZoneOffset

/** How a programme picks between an exercise and its variants. */
enum class VariantRule {
    /** Always the exercise itself. */
    NONE,
    /**
     * The exercise for [Program.variantEveryWeeks] weeks, then its variant (week B) for as many,
     * in turn: with 1, the variant on even weeks.
     */
    ALTERNATE_WEEKLY
}

/** A training programme: its workouts rotate in order; its weeks count from [startDate]. */
data class Program(
    val id: String,
    /** The name the user gave it; null for the seeded plan. */
    val name: String?,
    val startDate: LocalDate,
    /** Every Nth week is a deload week; null for none. */
    val deloadEveryWeeks: Int?,
    val variantRule: VariantRule,
    /** A deload week's weight, as a percentage of the usual one. */
    val deloadLoadPercent: Int = DEFAULT_DELOAD_LOAD_PERCENT,
    /** A deload week's sets per exercise; an exercise planned with fewer keeps its own. */
    val deloadSets: Int = DEFAULT_DELOAD_SETS,
    /** How many weeks the exercise, then its variant, last under [VariantRule.ALTERNATE_WEEKLY]. */
    val variantEveryWeeks: Int = 1
) {
    /** 1-based programme week containing [atMs]. Weeks start at midnight UTC of [startDate]. */
    fun weekNumber(atMs: Long): Int =
        ((atMs - startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) / WEEK_MS).toInt() + 1

    /** Never before week 1: a session from before the programme began was no deload. */
    fun isDeloadWeek(atMs: Long): Boolean = deloadEveryWeeks?.let { weekNumber(atMs).let { w -> w >= 1 && w % it == 0 } } ?: false

    /** Weeks until the next deload: 0 during one, null when the programme has none. */
    fun weeksUntilDeload(atMs: Long): Int? = deloadEveryWeeks?.let { every -> (every - weekNumber(atMs) % every) % every }

    /** True in the weeks the programme does exercises' variants (week B). */
    fun usesVariants(atMs: Long): Boolean {
        if (variantRule != VariantRule.ALTERNATE_WEEKLY) return false
        val week = weekNumber(atMs)
        return week >= 1 && ((week - 1) / variantEveryWeeks.coerceAtLeast(1)) % 2 == 1
    }

    /** The sets an exercise planned with [planned] gets in a deload week. */
    fun deloadSetsFor(planned: Int): Int = minOf(planned, deloadSets)

    companion object {
        const val DEFAULT_DELOAD_LOAD_PERCENT = 60
        const val DEFAULT_DELOAD_SETS = 2
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}

/** A programme as the programmes list shows it. */
data class ProgramSummary(
    val program: Program,
    val isActive: Boolean,
    /** When the user archived it; null when it isn't. */
    val archivedAt: Long?,
    /** Workouts in its rotation. */
    val workoutCount: Int,
    /** Sessions done in its workouts; a programme with any is archived, never deleted. */
    val sessionCount: Int,
    /** The plan the app seeds; it can be archived but not deleted, or the next start would seed it again. */
    val isSeeded: Boolean
) {
    val canDelete: Boolean get() = !isActive && !isSeeded && sessionCount == 0
}
