package com.doron.gymtracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.doron.gymtracker.MainActivity
import com.doron.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CardioTrackingService : Service() {

    companion object {
        private const val CHANNEL_ID = "cardio_tracking"
        private const val NOTIFICATION_ID = 1002
        private const val EXTRA_TYPE = "type"

        @Volatile var elapsedSecs = 0L
            private set
        @Volatile var distanceMeters = 0.0
        @Volatile var activityType = "RUNNING"

        fun start(context: Context, type: String) {
            activityType = type
            elapsedSecs = 0
            distanceMeters = 0.0
            context.startForegroundService(
                Intent(context, CardioTrackingService::class.java).apply {
                    putExtra(EXTRA_TYPE, type)
                }
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CardioTrackingService::class.java))
        }

        fun updateDistance(meters: Double) {
            distanceMeters = meters
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activityType = intent?.getStringExtra(EXTRA_TYPE) ?: "RUNNING"
        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            while (true) {
                delay(1_000)
                elapsedSecs++
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        elapsedSecs = 0
        distanceMeters = 0.0
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

    private fun buildNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val typeName = if (activityType == "RUNNING") "ריצה" else "הליכה"
        val time = formatTime(elapsedSecs)
        val distKm = distanceMeters / 1000.0

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$typeName — $time")
            .setContentText("%.2f ק״מ".format(distKm))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification())
    }

    private fun formatTime(secs: Long): String {
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }
}
