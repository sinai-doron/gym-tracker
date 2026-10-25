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

class SessionTimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "session_timer"
        private const val NOTIFICATION_ID = 1001

        @Volatile var sessionElapsedSecs = 0L
            private set
        @Volatile var restRemainingSecs: Int? = null
        @Volatile var workoutName: String = ""
        @Volatile var currentExercise: String = ""
        @Volatile var setInfo: String = ""

        fun start(context: Context, name: String) {
            workoutName = name
            sessionElapsedSecs = 0
            restRemainingSecs = null
            context.startForegroundService(Intent(context, SessionTimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionTimerService::class.java))
        }

        fun updateExerciseInfo(exercise: String, sets: String) {
            currentExercise = exercise
            setInfo = sets
        }

        fun updateRest(secs: Int?) {
            restRemainingSecs = secs
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            while (true) {
                delay(1_000)
                sessionElapsedSecs++
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        sessionElapsedSecs = 0
        restRemainingSecs = null
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "אימון פעיל",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "טיימר אימון ומנוחה"
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

        val sessionTime = formatTime(sessionElapsedSecs)
        val rest = restRemainingSecs

        val title = "$workoutName — $sessionTime"
        val text = buildString {
            if (rest != null && rest > 0) {
                append("מנוחה ${formatTime(rest.toLong())}")
                if (currentExercise.isNotEmpty()) append(" · $currentExercise")
            } else if (currentExercise.isNotEmpty()) {
                append(currentExercise)
                if (setInfo.isNotEmpty()) append(" · $setInfo")
            } else {
                append("אימון פעיל")
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
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
        val m = secs / 60
        val s = secs % 60
        return "%02d:%02d".format(m, s)
    }
}
