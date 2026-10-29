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
import com.doron.gymtracker.data.RunAnnouncements
import com.doron.gymtracker.data.RunMilestone
import com.doron.gymtracker.data.RunMilestones
import com.doron.gymtracker.data.prefs.AudioFeedback
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.media.RestTimerBeepPlayer
import com.doron.gymtracker.media.RunVoice
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
        private const val MILESTONE_CHECK_MS = 1_000L

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
    @Inject lateinit var voice: RunVoice
    @Inject lateinit var settingsPrefs: SettingsPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notifyJob: Job? = null
    private var milestonesJob: Job? = null
    private val milestones = RunMilestones()
    @Volatile private var audioFeedback = AudioFeedback.VOICE

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
        if (milestonesJob == null) {
            scope.launch {
                settingsPrefs.audioFeedback.collect {
                    audioFeedback = it
                    if (it == AudioFeedback.VOICE) voice.warmUp()
                }
            }
            // Polled rather than event-driven: a duration target is reached by the clock, not a fix
            milestonesJob = scope.launch {
                var wasPaused = false
                while (true) {
                    recorder.run.value?.let { run ->
                        val now = System.currentTimeMillis()
                        milestones.check(run, now).forEach { announce(it, run, now) }
                        // Auto-pause stops and restarts the notification's clock
                        val paused = run.clock.isPaused(now)
                        if (paused != wasPaused) {
                            wasPaused = paused
                            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(run))
                        }
                    }
                    delay(MILESTONE_CHECK_MS)
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    private fun announce(milestone: RunMilestone, run: CardioRun, now: Long) {
        Log.i(TAG, "Milestone: $milestone")
        when (audioFeedback) {
            AudioFeedback.OFF -> Unit
            AudioFeedback.VOICE -> if (voice.unavailable) tone(milestone) else {
                voice.speak(RunAnnouncements.text(milestone, run, now))
                if (milestone is RunMilestone.LapDone || milestone == RunMilestone.TargetReached) {
                    RestTimerBeepPlayer.vibrate()
                }
            }
            AudioFeedback.TONES -> tone(milestone)
        }
    }

    private fun tone(milestone: RunMilestone) = when (milestone) {
        is RunMilestone.LapDone -> RestTimerBeepPlayer.playBeep()
        RunMilestone.TargetReached -> RestTimerBeepPlayer.playCelebration()
        RunMilestone.Paused -> RestTimerBeepPlayer.playPause()
        RunMilestone.Resumed -> RestTimerBeepPlayer.playResume()
    }

    override fun onDestroy() {
        scope.cancel()
        voice.shutdown()
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
        val now = System.currentTimeMillis()
        val clock = run?.clock
        val paused = clock?.isPaused(now) == true

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (paused) "$typeName · ⏸ מושהה" else typeName)
            .setContentText("%.2f ק״מ".format(distKm))
            // The system ticks the clock itself, so the notification needn't update every second;
            // it counts moving time, so it starts as far back as the run has been moving
            .setUsesChronometer(clock != null && !paused)
            .setShowWhen(clock != null && !paused)
            .setWhen(clock?.let { now - it.activeMs(now) } ?: now)
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
