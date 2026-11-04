package com.doron.gymtracker.domain.model

import java.time.LocalDate
import java.time.ZoneOffset

/** How a programme picks between an exercise and its variants. */
enum class VariantRule {
    /** Always the exercise itself. */
    NONE,
    /** The variant on even weeks of the programme (week B), the exercise on odd ones. */
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
    val variantRule: VariantRule
) {
    /** 1-based programme week containing [atMs]. Weeks start at midnight UTC of [startDate]. */
    fun weekNumber(atMs: Long): Int =
        ((atMs - startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()) / WEEK_MS).toInt() + 1

    fun isDeloadWeek(atMs: Long): Boolean = deloadEveryWeeks?.let { weekNumber(atMs) % it == 0 } ?: false

    /** Weeks until the next deload: 0 during one, null when the programme has none. */
    fun weeksUntilDeload(atMs: Long): Int? = deloadEveryWeeks?.let { every -> (every - weekNumber(atMs) % every) % every }

    /** True in the weeks the programme does exercises' variants (week B). */
    fun usesVariants(atMs: Long): Boolean = variantRule == VariantRule.ALTERNATE_WEEKLY && weekNumber(atMs) % 2 == 0

    private companion object {
        const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
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
