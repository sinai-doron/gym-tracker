package com.doron.gymtracker.data

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
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

    val isAvailable: Boolean get() = stepSensor != null

    private var baseline: Float = 0f
    private var baselineDate: Long = 0L

    suspend fun start() {
        if (stepSensor == null) return
        val prefs = context.stepDataStore.data.first()
        baseline = prefs[KEY_BASELINE] ?: 0f
        baselineDate = prefs[KEY_BASELINE_DATE] ?: 0L
        sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val totalSteps = event.values[0]
        val todayEpoch = LocalDate.now(ZoneId.systemDefault()).toEpochDay()

        if (baselineDate != todayEpoch) {
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
