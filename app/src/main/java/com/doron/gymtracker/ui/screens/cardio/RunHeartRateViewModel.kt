package com.doron.gymtracker.ui.screens.cardio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.DistanceMark
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.HeartRate
import com.doron.gymtracker.data.HeartRateSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What a run summary shows of heart rate. */
sealed interface RunHeartRate {
    /** Nothing: no Health Connect, no readings during the run, or not looked yet. */
    data object Hidden : RunHeartRate
    /** Health Connect is there but may not be read: offer to connect a watch's heart rate. */
    data object CanAsk : RunHeartRate
    data class Shown(val summary: HeartRateSummary) : RunHeartRate
}

/**
 * A run's heart rate, read from Health Connect whenever the run is shown. A watch often syncs
 * minutes after a run ends, so it is read again when the screen comes back ([refresh]).
 */
@HiltViewModel
class RunHeartRateViewModel @Inject constructor(
    private val healthConnect: HealthConnectManager
) : ViewModel() {

    private val _state = MutableStateFlow<RunHeartRate>(RunHeartRate.Hidden)
    val state: StateFlow<RunHeartRate> = _state.asStateFlow()

    private var run: Run? = null

    private data class Run(val startedAt: Long, val finishedAt: Long, val marks: List<DistanceMark>)

    fun load(startedAt: Long, finishedAt: Long, marks: List<DistanceMark>) {
        val next = Run(startedAt, finishedAt, marks)
        if (next == run) return
        run = next
        refresh()
    }

    fun refresh() {
        val r = run ?: return
        viewModelScope.launch {
            val samples = healthConnect.readHeartRate(r.startedAt, r.finishedAt)
            _state.value = when {
                samples != null -> HeartRate.summary(samples, r.startedAt, r.finishedAt, r.marks)
                    ?.let { RunHeartRate.Shown(it) } ?: RunHeartRate.Hidden
                healthConnect.canAskForHeartRate() -> RunHeartRate.CanAsk
                else -> RunHeartRate.Hidden
            }
        }
    }
}
