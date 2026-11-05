package com.doron.gymtracker.data

import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {

    private val km = DistanceUnit.KM
    private val mi = DistanceUnit.MI

    @Test
    fun `distances pace and speed in kilometres or miles`() {
        assertEquals(5.0, km.of(5_000.0), 1e-9)
        assertEquals(3.107, mi.of(5_000.0), 0.001)
        assertEquals(1609.344, mi.toMeters(1.0), 1e-9)
        // 5:00 a km is 8:03 a mile; 12 km/h is 7.46 mph
        assertEquals(482.8, mi.pace(300.0), 0.1)
        assertEquals(300.0, km.pace(300.0), 1e-9)
        assertEquals(7.456, mi.speed(12.0), 0.001)
        // Climb in feet with miles
        assertEquals(328.1, mi.height(100.0), 0.1)
        assertEquals(100.0, km.height(100.0), 1e-9)
    }

    @Test
    fun `lap lengths pair up place by place, so changing units keeps the choice`() {
        assertEquals(listOf(0, 201, 402, 805, 1609, 3219, 8047), mi.lapOptions())
        assertEquals(km.lapOptions().size, mi.lapOptions().size)
        assertEquals(1609, km.lapIn(mi, 1000))
        assertEquals(1000, mi.lapIn(km, 1609))
        assertEquals(402, km.lapIn(mi, 400))
        assertEquals(0, km.lapIn(mi, 0))
        // Not an option: kept as it was
        assertEquals(1234, km.lapIn(mi, 1234))
    }

    @Test
    fun `a lap is one of the runner's units unless they chose otherwise`() {
        assertEquals(1000, SettingsPrefs.defaultLap(km))
        assertEquals(1609, SettingsPrefs.defaultLap(mi))
    }

    @Test
    fun `whole units of a lap, for "mile 3" and "kilometre 3"`() {
        assertEquals(1, km.wholeUnits(1000))
        assertEquals(2, km.wholeUnits(2000))
        assertNull(km.wholeUnits(400))
        assertEquals(1, mi.wholeUnits(1609))
        assertEquals(2, mi.wholeUnits(3219))
        assertNull(mi.wholeUnits(1000))
        assertNull(km.wholeUnits(0))
    }

    @Test
    fun `a distance target moves by half a unit, snapped to the half`() {
        assertEquals(5.5f, km.stepTargetKm(5f, 1), 1e-4f)
        assertEquals(4.5f, km.stepTargetKm(5f, -1), 1e-4f)
        // 5 km is 3.1 mi: up to 3.5 mi, down to 3 mi
        assertEquals(3.5, mi.of(mi.stepTargetKm(5f, 1) * 1000.0), 1e-4)
        assertEquals(3.0, mi.of(mi.stepTargetKm(5f, -1) * 1000.0), 1e-4)
        // From a whole half it moves a whole step, though the km kept is a hair off
        val threeAndAHalf = mi.stepTargetKm(5f, 1)
        assertEquals(4.0, mi.of(mi.stepTargetKm(threeAndAHalf, 1) * 1000.0), 1e-4)
        assertEquals(3.0, mi.of(mi.stepTargetKm(threeAndAHalf, -1) * 1000.0), 1e-4)
        // Never below half a unit
        assertEquals(0.5, mi.of(mi.stepTargetKm(0.8f, -1) * 1000.0), 1e-4)
        assertEquals(0.5f, km.stepTargetKm(0.5f, -1), 1e-4f)
    }

    @Test
    fun `weights typed in pounds are kept in kg and read back as typed`() {
        val lb = WeightUnit.LB
        assertEquals(61.235f, lb.toKg(135.0), 0.001f)
        assertEquals("135", lb.format(lb.toKg(135.0)))
        assertEquals("137.5", lb.format(lb.toKg(137.5)))
        assertEquals("45", lb.format(lb.toKg(45.0)))
        // A weight logged in kg reads to a tenth of a pound
        assertEquals("138.9", lb.format(63f))
        // kg as it was entered
        assertEquals("62.5", WeightUnit.KG.format(62.5f))
        assertEquals("80", WeightUnit.KG.format(80f))
        assertEquals(80f, WeightUnit.KG.toKg(80.0), 0f)
    }

    @Test
    fun `a step in pounds is a plate a lifter in pounds adds`() {
        val lb = WeightUnit.LB
        assertEquals(5.0, lb.step(2.5), 0.0)
        assertEquals(10.0, lb.step(5.0), 0.0)
        assertEquals(2.5, lb.step(1.25), 0.0)
        assertEquals(2.5, lb.step(1.0), 0.0)
        assertEquals(2.5, WeightUnit.KG.step(2.5), 0.0)
    }
}
