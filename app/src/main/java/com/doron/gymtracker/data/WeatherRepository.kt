package com.doron.gymtracker.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.model.WeatherCondition
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Current weather from Open-Meteo (free, no API key) for the phone's approximate location. */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var cached: Weather? = null
    private var cachedAt = 0L

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun currentWeather(): Weather? {
        cached?.let { if (System.currentTimeMillis() - cachedAt < CACHE_MS) return it }
        if (!hasLocationPermission()) return null
        return try {
            val location = approximateLocation()
            if (location == null) {
                Log.w("Weather", "No location available")
                return cached
            }
            fetch(location).also {
                cached = it
                cachedAt = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.w("Weather", "Failed to load weather", e)
            cached
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun approximateLocation(): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        // A fresh fix can hang indefinitely (no signal, location off), so fall back to the last known one
        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).awaitOrNull()
        } ?: client.lastLocation.awaitOrNull()
    }

    private suspend fun fetch(location: Location): Weather = withContext(Dispatchers.IO) {
        // ~1 km precision is plenty for weather and keeps the exact location private
        val lat = "%.2f".format(Locale.US, location.latitude)
        val lon = "%.2f".format(Locale.US, location.longitude)
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val current = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            Weather(
                temperatureC = current.getDouble("temperature_2m").roundToInt(),
                highC = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                lowC = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                condition = WeatherCondition.fromWmoCode(current.getInt("weather_code"))
            )
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun <T> Task<T>.awaitOrNull(): T? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener {
            Log.w("Weather", "Location request failed", it)
            cont.resume(null)
        }
        addOnCanceledListener { cont.resume(null) }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
        const val TIMEOUT_MS = 10_000
        const val LOCATION_TIMEOUT_MS = 10_000L
    }
}
