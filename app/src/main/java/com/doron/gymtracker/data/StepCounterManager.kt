package com.doron.gymtracker.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private val Context.stepDataStore by preferencesDataStore(name = "step_counter")
private val KEY_BASELINE = floatPreferencesKey("baseline")
private val KEY_BASELINE_DATE = longPreferencesKey("baseline_date")

@Singleton
class StepCounterManager @Inject constructor(
    @ApplicationContext private val context: Context
) : SensorEventListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    // Android 10+ delivers no step events without the ACTIVITY_RECOGNITION runtime permission
    val isAvailable: Boolean get() = stepSensor != null && hasActivityPermission()

    private var isListening = false

    private var baseline: Float = 0f
    private var baselineDate: Long = 0L

    suspend fun start() {
        if (!isAvailable || isListening) return
        isListening = true
        val prefs = context.stepDataStore.data.first()
        baseline = prefs[KEY_BASELINE] ?: 0f
        baselineDate = prefs[KEY_BASELINE_DATE] ?: 0L
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isListening = false
    }

    /** Each reading of the phone's step counter (steps since boot) while collected; nothing without permission. */
    fun counterUpdates(): Flow<Long> = callbackFlow {
        if (!isAvailable) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }

    private fun hasActivityPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSteps = event.values[0]
        val todayEpoch = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

        // The sensor counts since boot: a new day, or a reboot (count went down), restarts the baseline
        if (baselineDate != todayEpoch || totalSteps < baseline) {
            baseline = totalSteps
            baselineDate = todayEpoch
            scope.launch {
                context.stepDataStore.edit {
                    it[KEY_BASELINE] = baseline
                    it[KEY_BASELINE_DATE] = baselineDate
                }
            }
        }

        _todaySteps.value = (totalSteps - baseline).toInt().coerceAtLeast(0)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
