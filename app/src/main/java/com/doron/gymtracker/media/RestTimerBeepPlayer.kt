package com.doron.gymtracker.media

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object RestTimerBeepPlayer {

    private const val BEEP_DURATION_MS = 300
    private const val VOLUME = 100
    private const val CELEBRATION_BEEPS = 3
    private const val CELEBRATION_GAP_MS = 450L

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun playBeep() = playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD)

    /** Two low notes: a run auto-paused. */
    fun playPause() = playTone(ToneGenerator.TONE_PROP_NACK)

    /** Two high notes: a run moving again. */
    fun playResume() = playTone(ToneGenerator.TONE_PROP_ACK)

    private fun playTone(tone: Int) {
        try {
            // STREAM_MUSIC follows the active output (e.g. Bluetooth); STREAM_ALARM always rings on the phone speaker
            val toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
            toneGen.startTone(tone, BEEP_DURATION_MS)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGen.release()
            }, (BEEP_DURATION_MS + 100).toLong())
        } catch (_: Exception) {}

        vibrate()
    }

    /** A longer, three-beep alert, e.g. for reaching a run's target. */
    fun playCelebration() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        repeat(CELEBRATION_BEEPS) { i ->
            handler.postDelayed({ playBeep() }, i * CELEBRATION_GAP_MS)
        }
    }

    fun vibrate() {
        val ctx = appContext ?: return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }
}
