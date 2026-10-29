package com.doron.gymtracker.data

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val speed: Float?,
    val altitude: Double?,
    val accuracy: Float?,
    val timestamp: Long
)

/** Raw GPS fixes. Stateless: distance and pace are worked out by [CardioRecorder]. */
@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                // Batched fixes (e.g. delivered late while the screen was off) all count
                result.locations.forEach { loc ->
                    trySend(
                        LocationPoint(
                            loc.latitude, loc.longitude,
                            if (loc.hasSpeed()) loc.speed else null,
                            if (loc.hasAltitude()) loc.altitude else null,
                            if (loc.hasAccuracy()) loc.accuracy else null,
                            loc.time
                        )
                    )
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 2_000L
    }
}
