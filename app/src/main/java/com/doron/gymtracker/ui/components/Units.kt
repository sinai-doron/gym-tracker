package com.doron.gymtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource
import com.doron.gymtracker.R
import com.doron.gymtracker.data.MILE_LAPS
import com.doron.gymtracker.data.height
import com.doron.gymtracker.data.of
import com.doron.gymtracker.data.pace
import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.data.speed
import com.doron.gymtracker.domain.model.RunTarget
import kotlin.math.roundToInt
import com.doron.gymtracker.data.lapOptions

/** Kilometres or miles, as chosen in About you; provided once at the root (MainActivity). */
val LocalDistanceUnit = staticCompositionLocalOf { DistanceUnit.KM }

/** "km" or "mi". */
@Composable
fun distanceUnitLabel(unit: DistanceUnit = LocalDistanceUnit.current): String =
    stringResource(if (unit == DistanceUnit.KM) R.string.units_km else R.string.units_mi)

/** "/km" or "/mi". */
@Composable
fun paceUnitLabel(): String =
    stringResource(if (LocalDistanceUnit.current == DistanceUnit.KM) R.string.units_per_km else R.string.units_per_mi)

/** "km/h" or "mph". */
@Composable
fun speedUnitLabel(): String =
    stringResource(if (LocalDistanceUnit.current == DistanceUnit.KM) R.string.units_kmh else R.string.units_mph)

/** Climb and elevation: "m" or "ft". */
@Composable
fun heightUnitLabel(): String =
    stringResource(if (LocalDistanceUnit.current == DistanceUnit.KM) R.string.units_m else R.string.units_ft)

/** [meters] as a number in the runner's unit, to [decimals] places: "5.00", "3.11". */
@Composable
fun distanceNumber(meters: Double, decimals: Int = 2): String =
    "%.${decimals}f".format(appLocale(), LocalDistanceUnit.current.of(meters))

/** [meters] with the runner's unit: "5.00 km", "3.11 mi". */
@Composable
fun distanceText(meters: Double, decimals: Int = 2): String =
    stringResource(R.string.units_value, distanceNumber(meters, decimals), distanceUnitLabel())

/** A pace kept per kilometre, as minutes and seconds per the runner's unit: "5:20", "8:35". */
@Composable
fun paceNumber(secsPerKm: Number): String = formatMinSec(LocalDistanceUnit.current.pace(secsPerKm.toDouble()))

/** A pace kept per kilometre, with its unit: "5:20 /km", "8:35 /mi". */
@Composable
fun paceText(secsPerKm: Number): String = stringResource(R.string.units_value, paceNumber(secsPerKm), paceUnitLabel())

/** A speed kept in km/h, with the runner's unit to one place: "10.8 km/h", "6.7 mph". */
@Composable
fun speedText(kmh: Double): String =
    stringResource(R.string.units_value, "%.1f".format(appLocale(), LocalDistanceUnit.current.speed(kmh)), speedUnitLabel())

/** Climb or elevation kept in metres, as a whole number in metres or feet: "120", "394". */
@Composable
fun heightNumber(meters: Double): String = "${LocalDistanceUnit.current.height(meters).roundToInt()}"

/** Climb or elevation with its unit: "120 m", "394 ft". */
@Composable
fun heightText(meters: Double): String = stringResource(R.string.units_value, heightNumber(meters), heightUnitLabel())

/**
 * A lap or a distance target's length, as short as it reads: "400 m", "1 km", "2.5 km"; in miles
 * the usual fractions, "¼ mi", "½ mi", "1 mi".
 */
@Composable
fun lengthText(meters: Double, unit: DistanceUnit = LocalDistanceUnit.current): String = when (unit) {
    DistanceUnit.KM ->
        if (meters < 1000) stringResource(R.string.units_value, "${meters.roundToInt()}", stringResource(R.string.units_m))
        else stringResource(R.string.units_value, trimmed(meters / 1000), distanceUnitLabel(unit))
    DistanceUnit.MI -> {
        val miles = unit.of(meters)
        // A mile lap is kept in whole metres, so an eighth of a mile is 201 m, not 201.168
        val fraction = FRACTIONS.entries.firstOrNull { kotlin.math.abs(it.key - miles) < 0.001 }?.value
        stringResource(R.string.units_value, fraction ?: trimmed(miles), distanceUnitLabel(unit))
    }
}

/**
 * How long a run's laps were, in the unit they were set in: a run with mile laps reads "Every
 * 1 mi" and one with kilometre laps "Every 1 km", whichever unit the runner uses now.
 */
@Composable
fun lapLengthText(lapMeters: Int): String {
    val unit = when (lapMeters) {
        in DistanceUnit.KM.lapOptions() -> DistanceUnit.KM
        in DistanceUnit.MI.lapOptions() -> DistanceUnit.MI
        else -> LocalDistanceUnit.current
    }
    return lengthText(lapMeters.toDouble(), unit)
}

/** A distance target, kept in km, as a number in the runner's unit: "5", "3.1". */
@Composable
fun targetNumber(target: RunTarget): String = trimmed(LocalDistanceUnit.current.of(target.distanceKm * 1000.0))

/** A distance target with its unit: "5 km", "3.1 mi". */
@Composable
fun targetText(target: RunTarget): String = stringResource(R.string.units_value, targetNumber(target), distanceUnitLabel())

private val FRACTIONS = mapOf(MILE_LAPS[1] to "⅛", MILE_LAPS[2] to "¼", MILE_LAPS[3] to "½")

/** One decimal at most, none when whole: "5", "2.5". */
private fun trimmed(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10.0}"
}

private fun formatMinSec(totalSecs: Double): String {
    val secs = totalSecs.roundToInt()
    return "%d:%02d".format(secs / 60, secs % 60)
}
