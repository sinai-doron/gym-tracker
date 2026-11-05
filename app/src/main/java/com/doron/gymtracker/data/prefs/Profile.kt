package com.doron.gymtracker.data.prefs

import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

enum class WeightUnit { KG, LB }

enum class DistanceUnit { KM, MI }

enum class WeekStart { SUNDAY, MONDAY }

enum class Sex { FEMALE, MALE }

/**
 * What first launch asks in "About you", kept on the phone. Distances show in the chosen unit
 * (ui/components/Units.kt); pounds are still only saved, not shown. Birth year and sex only refine
 * calorie estimates.
 */
data class Profile(
    val weightUnit: WeightUnit,
    val distanceUnit: DistanceUnit,
    val weekStart: WeekStart,
    val birthYear: Int? = null,
    val sex: Sex? = null,
) {
    companion object {
        // Where pounds and miles are the everyday units
        private val POUNDS = setOf("US", "LR", "MM")
        private val MILES = setOf("US", "GB", "LR", "MM")

        /** What most people in [locale]'s region would pick, so they can just tap Next. */
        fun defaultsFor(locale: Locale): Profile = Profile(
            weightUnit = if (locale.country in POUNDS) WeightUnit.LB else WeightUnit.KG,
            distanceUnit = if (locale.country in MILES) DistanceUnit.MI else DistanceUnit.KM,
            weekStart = if (WeekFields.of(locale).firstDayOfWeek == DayOfWeek.MONDAY) WeekStart.MONDAY else WeekStart.SUNDAY,
        )

        /** Years someone training could be born in, as of [thisYear]. */
        fun birthYears(thisYear: Int): IntRange = (thisYear - 100)..(thisYear - 10)
    }
}
