package com.doron.gymtracker.media

import android.media.AudioManager
import android.media.ToneGenerator

object RestTimerBeepPlayer {

    private const val BEEP_DURATION_MS = 150
    private const val VOLUME = 80

    fun playBeep() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, VOLUME)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MS)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (BEEP_DURATION_MS + 50).toLong())
        } catch (_: Exception) {
        }
    }
}
