package com.doron.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.media.MediaPermissionHelper
import com.doron.gymtracker.media.NowPlayingProvider
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Play/pause/skip for whatever music app is playing, or, until the app may see media
 * sessions, a prompt to allow it (which can be dismissed for good).
 */
@Composable
fun MusicControls(settingsPrefs: SettingsPrefs?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val nowPlaying by NowPlayingProvider.state.collectAsState()
    val scope = rememberCoroutineScope()
    val permDismissed by (settingsPrefs?.mediaPermissionDismissed ?: flowOf(false)).collectAsState(initial = false)
    var hasPermission by remember { mutableStateOf(MediaPermissionHelper.isNotificationListenerEnabled(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasPermission = MediaPermissionHelper.isNotificationListenerEnabled(context)
    }

    val playing = nowPlaying
    if (playing != null) {
        NowPlayingBar(state = playing, modifier = modifier)
    } else if (!hasPermission && !permDismissed) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "הפעל גישה למדיה כדי לשלוט במוזיקה",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = {
                MediaPermissionHelper.openNotificationListenerSettings(context)
            }) {
                Text("הפעל")
            }
            IconButton(onClick = {
                scope.launch { settingsPrefs?.dismissMediaPermission() }
            }) {
                Text("✕", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
