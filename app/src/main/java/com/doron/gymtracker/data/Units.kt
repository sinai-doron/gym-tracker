package com.doron.gymtracker.data

import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.data.prefs.WeightUnit
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Distances are kept in metres everywhere; these turn them into what the runner reads, in
 * kilometres or miles. Climb and elevation follow along: metres with kilometres, feet with miles.
 */
object Units {
    const val METERS_PER_MILE = 1609.344
    const val FEET_PER_METER = 3.28084
}

/** Metres in one of this unit. */
val DistanceUnit.meters: Double get() = when (this) {
    DistanceUnit.KM -> 1000.0
    DistanceUnit.MI -> Units.METERS_PER_MILE
}

/** [meters] in this unit: 5000 m is 5 km or 3.107 mi. */
fun DistanceUnit.of(meters: Double): Double = meters / this.meters

/** This many units in metres. */
fun DistanceUnit.toMeters(value: Double): Double = value * meters

/** A pace in seconds per kilometre as seconds per this unit. */
fun DistanceUnit.pace(secsPerKm: Double): Double = secsPerKm * meters / 1000

/** A speed in km/h as this unit per hour. */
fun DistanceUnit.speed(kmh: Double): Double = kmh * 1000 / meters

/** Climb and elevation, which are metres with kilometres and feet with miles. */
fun DistanceUnit.height(meters: Double): Double = when (this) {
    DistanceUnit.KM -> meters
    DistanceUnit.MI -> meters * Units.FEET_PER_METER
}

/**
 * Lap lengths offered in Settings, in metres; 0 turns laps off. The two lists pair up place by
 * place (200 m and an eighth of a mile, 1 km and 1 mi...) so changing units keeps the choice
 * ([lapIn]); miles are rounded to whole metres, as laps are kept.
 */
fun DistanceUnit.lapOptions(): List<Int> = when (this) {
    DistanceUnit.KM -> listOf(0, 200, 400, 500, 1000, 2000, 5000)
    DistanceUnit.MI -> MILE_LAPS.map { toMeters(it).roundToInt() }
}

/** The mile laps as fractions of a mile, for [lapOptions] and their labels. */
val MILE_LAPS = listOf(0.0, 0.125, 0.25, 0.5, 1.0, 2.0, 5.0)

/** How many whole units a lap is (a lap of exactly 1 or 2 km, 1 or 2 mi), or null for any other. */
fun DistanceUnit.wholeUnits(lapMeters: Int): Int? {
    if (lapMeters <= 0) return null
    val units = of(lapMeters.toDouble())
    val whole = units.roundToInt()
    // A mile lap is kept as 1609 m, so allow a metre either way
    return whole.takeIf { it > 0 && abs(toMeters(whole.toDouble()) - lapMeters) < 1.0 }
}

/**
 * The lap length in [to] that stands where [lapMeters] stood in this unit's options (1 km becomes
 * 1 mi), so changing units keeps the runner's choice; a length that isn't an option is kept.
 */
fun DistanceUnit.lapIn(to: DistanceUnit, lapMeters: Int): Int {
    val i = lapOptions().indexOf(lapMeters)
    return if (i >= 0) to.lapOptions()[i] else lapMeters
}


/**
 * A distance target, kept in km, moved by [steps] half units and snapped to the half first, so in
 * miles 5 km (3.1 mi) goes to 3.5 or 3 mi rather than 3.6 or 2.6. Never below half a unit.
 */
fun DistanceUnit.stepTargetKm(km: Float, steps: Int): Float {
    val halves = of(km * 1000.0) * 2
    // Kept as a Float in km, half a mile comes back a hair either side of a whole half
    val snapped = if (steps > 0) floor(halves + 1e-4) else ceil(halves - 1e-4)
    val next = (snapped + steps).coerceAtLeast(1.0)
    return (toMeters(next / 2) / 1000).toFloat()
}

/** Weights are kept in kg; these show them, and read what is typed, in kg or pounds. */
const val KG_PER_LB = 0.45359237

/** [kg] in this unit. */
fun WeightUnit.of(kg: Float): Double = when (this) {
    WeightUnit.KG -> kg.toDouble()
    WeightUnit.LB -> kg / KG_PER_LB
}

/** A weight typed in this unit, in kg as it is kept. */
fun WeightUnit.toKg(value: Double): Float = when (this) {
    WeightUnit.KG -> value.toFloat()
    WeightUnit.LB -> (value * KG_PER_LB).toFloat()
}

/**
 * The step a lifter in this unit adds for an equipment step kept in kg: plates in pounds go up by
 * 2.5 lb at the least, so 2.5 kg is 5 lb and 1.25 kg is 2.5 lb, not 5.51 and 2.76.
 */
fun WeightUnit.step(kgStep: Double): Double = when (this) {
    WeightUnit.KG -> kgStep
    WeightUnit.LB -> ((kgStep / KG_PER_LB) / 2.5).roundToInt().coerceAtLeast(1) * 2.5
}

/** A weight in this unit as it reads: kg as entered ("62.5", "1.25"), pounds to a tenth ("135", "137.5"). */
fun WeightUnit.format(kg: Float): String = when (this) {
    WeightUnit.KG -> if (kg == kg.toLong().toFloat()) kg.toLong().toString() else kg.toString()
    WeightUnit.LB -> {
        val tenths = (of(kg) * 10).roundToInt()
        if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${abs(tenths % 10)}"
    }
}

/**
 * [kg] rounded down to what plates make in this unit: 2.5 kg, or 5 lb. A deload's share of the
 * usual weight (60% of 62.5 kg is 37.5; of 135 lb, 81) becomes a weight the lifter can load.
 */
fun WeightUnit.floorToPlate(kg: Float): Float {
    val plate = if (this == WeightUnit.KG) 2.5 else 5.0
    // A hair over, so a weight already on a plate step isn't knocked down by float error
    return toKg(floor(of(kg) / plate + 1e-6) * plate)
}
