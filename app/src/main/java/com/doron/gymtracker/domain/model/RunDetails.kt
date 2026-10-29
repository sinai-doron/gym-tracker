package com.doron.gymtracker.domain.model

/** What a run was set up with, and the stats recorded for it. */
data class RunDetails(
    val sessionId: Long,
    val target: RunTarget,
    /** 0 when laps were off. */
    val lapMeters: Int,
    val stepCounterAtStart: Long? = null,
    val steps: Int? = null,
    val elevationGainM: Double? = null,
    val calories: Int? = null,
    val weatherTempC: Int? = null,
    val weatherCondition: WeatherCondition? = null
)
