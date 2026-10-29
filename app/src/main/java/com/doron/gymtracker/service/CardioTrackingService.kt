package com.doron.gymtracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.doron.gymtracker.MainActivity
import com.doron.gymtracker.R
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.CardioRun
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a run recording while the screen is off or the app is in the background. The
 * recording itself is done by [CardioRecorder]; this service is what keeps the process
 * (and GPS) alive, and shows the ongoing notification.
 */
@AndroidEntryPoint
class CardioTrackingService : Service() {

    companion object {
        private const val TAG = "CardioTrackingService"
        private const val CHANNEL_ID = "cardio_tracking"
        private const val NOTIFICATION_ID = 1002
        private const val EXTRA_SESSION_ID = "session_id"
        // Refresh the notification every 10 m, not on every GPS fix
        private const val NOTIFY_EVERY_M = 10

        fun start(context: Context, sessionId: Long) {
            context.startForegroundService(
                Intent(context, CardioTrackingService::class.java).putExtra(EXTRA_SESSION_ID, sessionId)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CardioTrackingService::class.java))
        }
    }

    @Inject lateinit var recorder: CardioRecorder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notifyJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, -1L) ?: -1L
        try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, buildNotification(recorder.run.value),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } catch (e: Exception) {
            // Android won't start location tracking from the background (e.g. a redelivered
            // start after the app was killed). The run is picked back up when the app opens.
            Log.w(TAG, "Couldn't start tracking", e)
            stopSelf()
            return START_NOT_STICKY
        }
        if (sessionId < 0) {
            stopSelf()
            return START_NOT_STICKY
        }
        recorder.start(sessionId)
        if (notifyJob == null) {
            notifyJob = scope.launch {
                recorder.run.filterNotNull()
                    .distinctUntilChangedBy { it.sessionId to (it.distanceMeters / NOTIFY_EVERY_M).toInt() }
                    .collect { run ->
                        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(run))
                    }
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        scope.cancel()
        recorder.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "מעקב ריצה/הליכה",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GPS פעיל בזמן אימון"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(run: CardioRun?): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val typeName = if (run?.type == "WALKING") "הליכה" else "ריצה"
        val distKm = (run?.distanceMeters ?: 0.0) / 1000.0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(typeName)
            .setContentText("%.2f ק״מ".format(distKm))
            // The system ticks the clock itself, so the notification needn't update every second
            .setUsesChronometer(run != null)
            .setShowWhen(run != null)
            .setWhen(run?.startedAt ?: System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setSilent(true)
            .build()
    }
}
