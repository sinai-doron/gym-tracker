package com.doron.gymtracker.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _totalDistance = MutableStateFlow(0.0)
    val totalDistance: StateFlow<Double> = _totalDistance.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _currentPace = MutableStateFlow("")
    val currentPace: StateFlow<String> = _currentPace.asStateFlow()

    private var lastLocation: Location? = null

    fun reset() {
        _totalDistance.value = 0.0
        _currentSpeed.value = 0f
        _currentPace.value = ""
        lastLocation = null
    }

    @SuppressLint("MissingPermission")
    fun locationUpdates(): Flow<LocationPoint> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val prev = lastLocation
                    if (prev != null) {
                        val dist = prev.distanceTo(loc)
                        if (dist > 1f) {
                            _totalDistance.value += dist
                        }
                    }
                    lastLocation = loc
                    _currentSpeed.value = loc.speed
                    if (loc.speed > 0.3f) {
                        val paceSecsPerKm = 1000.0 / loc.speed
                        val mins = (paceSecsPerKm / 60).toInt()
                        val secs = (paceSecsPerKm % 60).toInt()
                        _currentPace.value = "%d:%02d".format(mins, secs)
                    }

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
}
