package com.doron.gymtracker.ui.screens.cardio

import com.doron.gymtracker.data.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsLockStateTest {

    private fun fix(accuracy: Float?) =
        GpsLockState(position = LocationPoint(51.5, -0.12, speed = null, altitude = null, accuracy = accuracy, timestamp = 0))

    @Test
    fun `no fix yet shows one bar`() {
        assertEquals(1, GpsLockState().signalBars)
        assertEquals(1, fix(null).signalBars)
    }

    @Test
    fun `bars rise as the fix gets precise`() {
        assertEquals(1, fix(80f).signalBars)
        assertEquals(2, fix(30f).signalBars)
        assertEquals(3, fix(15f).signalBars)
        assertEquals(4, fix(5f).signalBars)
    }

    @Test
    fun `a fix precise enough to start never shows fewer than three bars`() {
        assertEquals(3, fix(GpsLockState.LOCK_ACCURACY_M - 0.1f).signalBars)
        assertEquals(2, fix(GpsLockState.LOCK_ACCURACY_M).signalBars)
    }
}
