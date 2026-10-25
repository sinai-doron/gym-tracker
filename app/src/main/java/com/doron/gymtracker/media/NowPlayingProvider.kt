package com.doron.gymtracker.media

import android.media.session.MediaController
import android.media.session.PlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NowPlayingProvider {

    private val _state = MutableStateFlow<NowPlayingState?>(null)
    val state: StateFlow<NowPlayingState?> = _state.asStateFlow()

    fun refresh(controllers: List<MediaController>) {
        val active = controllers.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: controllers.firstOrNull { it.metadata != null }

        if (active == null || active.metadata == null) {
            _state.value = null
            return
        }

        val metadata = active.metadata!!
        _state.value = NowPlayingState(
            title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
            albumArt = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART),
            isPlaying = active.playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = active
        )
    }

    fun clear() {
        _state.value = null
    }
}
