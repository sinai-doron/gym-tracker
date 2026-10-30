package com.doron.gymtracker.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doron.gymtracker.data.CardioRecorder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The run notification's pause and resume buttons. A broadcast rather than a service
 * command, so a stale button can't restart tracking or be replayed after a restart.
 */
@AndroidEntryPoint
class RunPauseReceiver : BroadcastReceiver() {

    @Inject lateinit var recorder: CardioRecorder

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE -> recorder.pause()
            ACTION_RESUME -> recorder.resume()
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.doron.gymtracker.action.PAUSE_RUN"
        const val ACTION_RESUME = "com.doron.gymtracker.action.RESUME_RUN"
    }
}
