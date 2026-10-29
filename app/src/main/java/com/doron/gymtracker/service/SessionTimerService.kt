package com.doron.gymtracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

        @Volatile private var sessionStartedAt = 0L
        val sessionElapsedSecs: Long
            get() = if (sessionStartedAt == 0L) 0L else (System.currentTimeMillis() - sessionStartedAt) / 1000
        @Volatile var restRemainingSecs: Int? = null
        @Volatile var workoutName: String = ""
        @Volatile var currentExercise: String = ""
        @Volatile var setInfo: String = ""

        fun start(context: Context, name: String, startedAt: Long) {
            workoutName = name
            sessionStartedAt = startedAt
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
                updateNotification()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        sessionStartedAt = 0
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
        val largeIcon = circularBitmap(BitmapFactory.decodeResource(resources, R.drawable.ic_app_logo))

        val title: String
        val text: String
        if (rest != null && rest > 0) {
            title = "מנוחה ${formatTime(rest.toLong())}"
            text = "$workoutName — $sessionTime"
        } else {
            title = "$workoutName — $sessionTime"
            text = if (currentExercise.isNotEmpty()) {
                if (setInfo.isNotEmpty()) "$currentExercise · $setInfo" else currentExercise
            } else {
                "אימון פעיל"
            }
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
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

    private fun circularBitmap(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return output
    }

    private fun formatTime(secs: Long): String {
        val m = secs / 60
        val s = secs % 60
        return "%02d:%02d".format(m, s)
    }
}
