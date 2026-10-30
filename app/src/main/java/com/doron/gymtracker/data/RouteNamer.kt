package com.doron.gymtracker.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.doron.gymtracker.domain.model.RoutePoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * Names a route after where it starts ("לונדון", "זכרון יעקב"), in Hebrew. Asks OpenStreetMap
 * (whose places have Hebrew names), and falls back to the phone's own geocoder.
 */
@Singleton
class RouteNamer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** The place's name, or null if the phone can't tell (offline, no geocoder). */
    suspend fun placeName(start: RoutePoint): String? =
        runCatching { openStreetMapName(start) }.onFailure { Log.w(TAG, "OpenStreetMap lookup failed", it) }.getOrNull()
            ?: geocoderName(start)

    /**
     * The street at [p] ("Berners Street", "הרצל"), or null offline. Remembered for the app's
     * lifetime, so opening a route again doesn't ask OpenStreetMap again.
     */
    suspend fun streetName(p: RoutePoint): String? {
        val key = "%.4f,%.4f".format(Locale.US, p.latitude, p.longitude)
        streets[key]?.let { return it }
        val name = withContext(Dispatchers.IO) {
            runCatching {
                // Spread back-to-back lookups (start then finish) to OpenStreetMap's one a second
                synchronized(this@RouteNamer) {
                    val wait = lastRequestAt + NOMINATIM_GAP_MS - System.currentTimeMillis()
                    if (wait > 0) Thread.sleep(wait)
                    lastRequestAt = System.currentTimeMillis()
                }
                val address = reverse(p, ZOOM_STREET, decimals = 4) ?: return@runCatching null
                listOf("road", "pedestrian", "footway", "path", "neighbourhood", "suburb")
                    .firstNotNullOfOrNull { key -> address.optString(key).takeIf { it.isNotBlank() } }
            }.onFailure { Log.w(TAG, "Street lookup failed", it) }.getOrNull()
        }
        name?.let { streets[key] = it }
        return name
    }

    private val streets = java.util.concurrent.ConcurrentHashMap<String, String>()
    @Volatile private var lastRequestAt = 0L

    /**
     * A big city's name if the start is in one (a route in Westminster is "לונדון"), else the
     * town or village (Zichron Ya'akov rather than its district).
     */
    private suspend fun openStreetMapName(p: RoutePoint): String? = withContext(Dispatchers.IO) {
        val city = reverse(p, ZOOM_CITY)?.optString("city")?.takeIf { it.isNotBlank() }
        if (city != null) return@withContext city
        // OpenStreetMap allows one request a second
        delay(NOMINATIM_GAP_MS)
        val address = reverse(p, ZOOM_TOWN) ?: return@withContext null
        listOf("city", "town", "village", "municipality", "suburb")
            .firstNotNullOfOrNull { key -> address.optString(key).takeIf { it.isNotBlank() } }
    }

    private fun reverse(p: RoutePoint, zoom: Int, decimals: Int = 3): JSONObject? {
        // ~100 m is plenty to name a place (a street needs ~10 m)
        val lat = "%.${decimals}f".format(Locale.US, p.latitude)
        val lon = "%.${decimals}f".format(Locale.US, p.longitude)
        val url = URL("https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=$zoom&accept-language=he")
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = HTTP_TIMEOUT_MS
            connection.readTimeout = HTTP_TIMEOUT_MS
            // OpenStreetMap's usage policy asks every app to identify itself
            connection.setRequestProperty("User-Agent", "GymTracker/1.0 (personal Android app)")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONObject("address")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun geocoderName(start: RoutePoint): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.forLanguageTag("he"))
        val addresses = withTimeoutOrNull(TIMEOUT_MS) { lookup(geocoder, start) }.orEmpty()
        // A town or city from any of the matches beats a county or region ("Greater London")
        fun first(of: (Address) -> String?) = addresses.firstNotNullOfOrNull { of(it)?.takeIf(String::isNotBlank) }
        return first { it.locality } ?: first { it.subLocality } ?: first { it.subAdminArea } ?: first { it.adminArea }
    }

    private suspend fun lookup(geocoder: Geocoder, p: RoutePoint): List<Address>? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(p.latitude, p.longitude, MAX_RESULTS, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) {
                        Log.w(TAG, "Couldn't name the place: $errorMessage")
                        cont.resume(null)
                    }
                })
            }
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(p.latitude, p.longitude, MAX_RESULTS) }.getOrNull()
        }

    private companion object {
        const val TAG = "RouteNamer"
        const val TIMEOUT_MS = 8_000L
        const val MAX_RESULTS = 5
        const val ZOOM_CITY = 8
        const val ZOOM_TOWN = 10
        const val ZOOM_STREET = 17
        const val NOMINATIM_GAP_MS = 1_100L
        const val HTTP_TIMEOUT_MS = 8_000
    }
}
