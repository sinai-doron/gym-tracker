package com.doron.gymtracker.domain.model

/** The kinds of personal record; the fastest ones are for a stretch of [meters] within any run. */
enum class RecordKind(val meters: Double? = null) {
    FASTEST_1K(1_000.0),
    FASTEST_5K(5_000.0),
    FASTEST_10K(10_000.0),
    LONGEST_RUN,
    BEST_WEEK;

    val isTime: Boolean get() = meters != null
}

/**
 * A personal record: seconds for the fastest kinds, meters for the longest run and the
 * best week. [sessionId] is the run that set it (null for a week); [at] is when that run
 * started, or the Sunday the week began.
 */
data class PersonalRecord(
    val kind: RecordKind,
    val value: Double,
    val sessionId: Long?,
    val at: Long
)
