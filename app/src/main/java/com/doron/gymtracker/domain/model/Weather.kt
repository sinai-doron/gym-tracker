package com.doron.gymtracker.domain.model

data class Weather(
    val temperatureC: Int,
    val highC: Int,
    val lowC: Int,
    val condition: WeatherCondition
)

enum class WeatherCondition(val emoji: String) {
    CLEAR("☀️"),
    PARTLY_CLOUDY("⛅"),
    CLOUDY("☁️"),
    FOG("🌫️"),
    DRIZZLE("🌦️"),
    RAIN("🌧️"),
    SNOW("❄️"),
    THUNDERSTORM("⛈️");

    companion object {
        /** Maps a WMO weather interpretation code (as used by Open-Meteo). */
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            in 61..67, in 80..82 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 95..99 -> THUNDERSTORM
            else -> CLOUDY
        }
    }
}
