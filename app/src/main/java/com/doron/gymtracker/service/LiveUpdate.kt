package com.doron.gymtracker.service

import android.app.Notification
import android.os.Bundle
import androidx.core.app.NotificationCompat

/**
 * Asks Android 16+ to show the notification as a Live Update: at the top of the shade, expanded
 * on the lock screen, and as a chip in the status bar with its clock, like a media player's.
 * Older versions ignore the extra. The system promotes it only while it stays ongoing, has a
 * title, isn't colorized and its channel isn't IMPORTANCE_MIN; the user can still turn it off
 * per app. Written as the extra itself, as androidx.core 1.15 has no builder method for it.
 */
fun NotificationCompat.Builder.requestLiveUpdate(): NotificationCompat.Builder =
    addExtras(Bundle().apply { putBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true) })
