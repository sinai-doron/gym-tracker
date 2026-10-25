package com.doron.gymtracker.media

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MediaSessionService : NotificationListenerService() {

    private var sessionManager: MediaSessionManager? = null
    private val controllerCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            onActiveSessionsChanged(controllers ?: emptyList())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
        val component = ComponentName(this, MediaSessionService::class.java)
        try {
            val controllers = sessionManager?.getActiveSessions(component) ?: emptyList()
            onActiveSessionsChanged(controllers)
            sessionManager?.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
        } catch (_: SecurityException) {
            NowPlayingProvider.clear()
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        unregisterAllCallbacks()
        sessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        NowPlayingProvider.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun onActiveSessionsChanged(controllers: List<MediaController>) {
        unregisterAllCallbacks()
        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                    refreshFromManager()
                }
                override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                    refreshFromManager()
                }
            }
            controller.registerCallback(callback)
            controllerCallbacks[controller] = callback
        }
        NowPlayingProvider.refresh(controllers)
    }

    private fun refreshFromManager() {
        val component = ComponentName(this, MediaSessionService::class.java)
        try {
            val controllers = sessionManager?.getActiveSessions(component) ?: emptyList()
            NowPlayingProvider.refresh(controllers)
        } catch (_: SecurityException) {
            NowPlayingProvider.clear()
        }
    }

    private fun unregisterAllCallbacks() {
        controllerCallbacks.forEach { (controller, callback) ->
            controller.unregisterCallback(callback)
        }
        controllerCallbacks.clear()
    }
}
