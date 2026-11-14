package com.doron.gymtracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.doron.gymtracker.MainActivity
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.BodyPrefs
import com.doron.gymtracker.data.repository.WaterRepository
import com.doron.gymtracker.data.repository.WeightRepository
import com.doron.gymtracker.domain.model.Reminders
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Rings the weigh-in or water reminder, then sets up the next one. A weigh-in reminder stays quiet
 * once today's weigh-in is in; a water one while the day is on pace, when asked. Set up again
 * whenever the settings change and when the app starts ([schedule]).
 */
@HiltWorker
class BodyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val prefs: BodyPrefs,
    private val weight: WeightRepository,
    private val water: WaterRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val kind = inputData.getString(KIND) ?: return Result.success()
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val from = Reminders.startOfDay(today)
        val until = Reminders.startOfDay(today.plusDays(1))
        when (kind) {
            WEIGHT -> if (!weight.weighedInBetween(from, until)) {
                notify(WEIGHT, R.string.reminder_weight_title, R.string.reminder_weight_text, ROUTE_WEIGHT_LOG)
            }
            WATER -> {
                val r = prefs.waterReminder.first()
                val goal = water.goalNow()
                val total = water.totalBetween(from, until)
                val onTrack = Reminders.waterOnTrack(total, goal.dailyMl, r, now)
                if (total < goal.dailyMl && !(r.skipWhenOnTrack && onTrack)) {
                    notify(WATER, R.string.reminder_water_title, R.string.reminder_water_text, ROUTE_WATER)
                }
            }
        }
        // Chained after this run: replacing would cancel the very work that's running
        schedule(applicationContext, prefs, kind, fromWorker = true)
        return Result.success()
    }

    private fun notify(kind: String, title: Int, text: Int, route: String) {
        val context = applicationContext
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context, kind.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, route)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(title))
            .setContentText(context.getString(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(kind.hashCode(), notification)
    }

    companion object {
        const val WEIGHT = "weight"
        const val WATER = "water"
        const val ROUTE_WEIGHT_LOG = "weight/log"
        const val ROUTE_WATER = "water"
        private const val KIND = "kind"
        private const val CHANNEL = "body_reminders"

        /** Sets up [kind]'s next reminder, or clears it when the reminder is off. */
        suspend fun schedule(context: Context, prefs: BodyPrefs, kind: String, fromWorker: Boolean = false) {
            val now = LocalDateTime.now()
            val next = when (kind) {
                WEIGHT -> Reminders.nextWeighIn(prefs.weightReminder.first(), now)
                else -> Reminders.nextWater(prefs.waterReminder.first(), now)
            }
            val work = WorkManager.getInstance(context)
            val name = "body-reminder-$kind"
            if (next == null) {
                if (!fromWorker) work.cancelUniqueWork(name)
                return
            }
            val request = OneTimeWorkRequestBuilder<BodyReminderWorker>()
                .setInitialDelay(Reminders.millisUntil(next, now), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KIND to kind))
                .build()
            work.enqueueUniqueWork(name, if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE, request)
        }

        suspend fun scheduleAll(context: Context, prefs: BodyPrefs) {
            schedule(context, prefs, WEIGHT)
            schedule(context, prefs, WATER)
        }
    }
}
