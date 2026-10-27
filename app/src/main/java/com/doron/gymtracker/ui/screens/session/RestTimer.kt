package com.doron.gymtracker.ui.screens.session

import com.doron.gymtracker.media.RestTimerBeepPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RestState(val remainingSecs: Int, val totalSecs: Int)

/** The session's single rest countdown — starting a new rest replaces the running one. */
class RestTimer(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow<RestState?>(null)
    val state: StateFlow<RestState?> = _state.asStateFlow()

    private var job: Job? = null

    fun start(durationSecs: Int) = run(durationSecs, durationSecs)

    fun adjust(deltaSecs: Int) {
        val current = _state.value ?: return
        val newRemaining = (current.remainingSecs + deltaSecs).coerceAtLeast(5)
        val newTotal = (current.totalSecs + deltaSecs).coerceAtLeast(newRemaining)
        run(newRemaining, newTotal)
    }

    fun dismiss() {
        job?.cancel()
        job = null
        _state.value = null
    }

    private fun run(remainingSecs: Int, totalSecs: Int) {
        job?.cancel()
        _state.value = RestState(remainingSecs, totalSecs)
        job = scope.launch {
            var remaining = remainingSecs
            while (remaining > 0) {
                delay(1_000)
                remaining--
                if (remaining in 1..3) RestTimerBeepPlayer.playBeep()
                _state.value = if (remaining > 0) RestState(remaining, totalSecs) else null
            }
        }
    }
}
