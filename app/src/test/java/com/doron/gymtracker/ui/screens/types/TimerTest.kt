package com.doron.gymtracker.ui.screens.types

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerTest {
    @Test
    fun `the clock shows hours only once there are some`() {
        assertEquals("0:00", clock(0))
        assertEquals("4:05", clock(245))
        assertEquals("1:00:00", clock(3600))
        assertEquals("1:30:07", clock(5407))
    }
}
