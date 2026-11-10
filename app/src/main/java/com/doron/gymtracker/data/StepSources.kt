package com.doron.gymtracker.data

import java.time.LocalDate

/**
 * A day's steps when several apps write them to Health Connect. Health Connect's own total
 * adds sources it can't line up: the phone writes many short records through the day, and
 * Samsung Health writes the same steps again as one record for the whole day. Each source
 * counts the whole day on its own, so the day is the largest of them.
 */
object StepSources {
    fun perDay(byOrigin: Map<String, Map<LocalDate, Long>>): Map<LocalDate, Long> =
        byOrigin.values.flatMap { it.entries }
            .groupBy({ it.key }) { it.value }
            .mapValues { (_, counts) -> counts.max() }
}
