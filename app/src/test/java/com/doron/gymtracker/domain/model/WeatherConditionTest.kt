package com.doron.gymtracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherConditionTest {

    @Test
    fun `maps WMO codes to conditions`() {
        assertEquals(WeatherCondition.CLEAR, WeatherCondition.fromWmoCode(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherCondition.fromWmoCode(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(3))
        assertEquals(WeatherCondition.FOG, WeatherCondition.fromWmoCode(45))
        assertEquals(WeatherCondition.DRIZZLE, WeatherCondition.fromWmoCode(53))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(63))
        assertEquals(WeatherCondition.RAIN, WeatherCondition.fromWmoCode(81))
        assertEquals(WeatherCondition.SNOW, WeatherCondition.fromWmoCode(75))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherCondition.fromWmoCode(95))
    }

    @Test
    fun `unknown codes fall back to cloudy`() =
        assertEquals(WeatherCondition.CLOUDY, WeatherCondition.fromWmoCode(1234))
}
