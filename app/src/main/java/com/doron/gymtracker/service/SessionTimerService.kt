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
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.doron.gymtracker.MainActivity
import com.doron.gymtracker.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps a workout alive while the phone is locked, and shows it as an ongoing notification that
 * Android 16+ promotes to a Live Update (see [requestLiveUpdate]): the workout's clock in the
 * status bar chip, or the rest countdown while resting. The system ticks both clocks, so the
 * notification is posted again only when what it shows changes.
 */
class SessionTimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "session_timer"
        private const val NOTIFICATION_ID = 1001

        @Volatile private var sessionStartedAt = 0L
        /** When the running rest ends, in wall-clock millis; null when not resting. */
        @Volatile private var restEndsAt: Long? = null
        @Volatile var workoutName: String = ""
        @Volatile var currentExercise: String = ""

        fun start(context: Context, name: String, startedAt: Long) {
            workoutName = name
            sessionStartedAt = startedAt
            restEndsAt = null
            context.startForegroundService(Intent(context, SessionTimerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SessionTimerService::class.java))
        }

        fun updateExercise(exercise: String) {
            currentExercise = exercise
        }

        /** The rest's seconds left, every second while it runs; a jump of over a second (±30 s) moves its end. */
        fun updateRest(secs: Int?) {
            if (secs == null || secs <= 0) {
                restEndsAt = null
                return
            }
            val endsAt = System.currentTimeMillis() + secs * 1000L
            val current = restEndsAt
            if (current == null || kotlin.math.abs(endsAt - current) > 1_500) restEndsAt = endsAt
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var shown: List<Any?>? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        shown = content()
        startForeground(NOTIFICATION_ID, buildNotification())

        scope.launch {
            while (true) {
                delay(1_000)
                val now = content()
                if (now != shown) {
                    shown = now
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        sessionStartedAt = 0
        restEndsAt = null
        currentExercise = ""
        super.onDestroy()
    }

    /**
     * Everything the notification shows, to post it again only when one of them changes; with
     * whether notifications are allowed, so it appears as soon as the user allows them.
     */
    private fun content(): List<Any?> = listOf(
        workoutName, currentExercise, sessionStartedAt, restEndsAt,
        NotificationManagerCompat.from(this).areNotificationsEnabled()
    )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.session_active_workout),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.session_channel_description)
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
        val restEnd = restEndsAt
        val exercise = currentExercise.ifEmpty { getString(R.string.session_active_workout) }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .apply {
                if (restEnd != null) {
                    // Resting: counts down to the rest's end, and says what's next
                    setContentTitle(getString(R.string.rest_label))
                    setContentText("$workoutName · $exercise")
                    setWhen(restEnd)
                    setChronometerCountDown(true)
                } else {
                    // Training: counts up from the session's start
                    setContentTitle(workoutName.ifEmpty { getString(R.string.session_active_workout) })
                    setContentText(exercise)
                    setWhen(if (sessionStartedAt > 0) sessionStartedAt else System.currentTimeMillis())
                }
            }
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .requestLiveUpdate()
            .setSilent(true)
            .build()
    }
}
