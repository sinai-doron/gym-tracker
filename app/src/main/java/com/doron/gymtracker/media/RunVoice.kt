package com.doron.gymtracker.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.doron.gymtracker.AppLocale
import com.doron.gymtracker.data.VoiceLanguage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Speaks run announcements in the app's language (Hebrew, else English) through whatever is
 * playing audio (e.g. Bluetooth headphones), lowering any music while it talks.
 */
@Singleton
class RunVoice @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes)
        .build()

    private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var pending: String? = null

    /** The language this run's announcements are in, fixed when the engine starts. */
    @Volatile var language: VoiceLanguage = VoiceLanguage.of(AppLocale.language())
        private set

    /** True once the engine has started without [language]: callers should beep instead. */
    @Volatile var unavailable = false
        private set

    fun speak(text: String) {
        val engine = tts ?: create()
        if (!ready) {
            // The engine starts up asynchronously; say the latest line once it's ready
            pending = text
            return
        }
        audioManager.requestAudioFocus(focus)
        engine.speak(text, TextToSpeech.QUEUE_ADD, null, "run-${System.nanoTime()}")
    }

    /** Starts the engine ahead of the first announcement, so that one isn't delayed. */
    fun warmUp() {
        if (tts == null) create()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun create(): TextToSpeech = TextToSpeech(context) { status ->
        val engine = tts ?: return@TextToSpeech
        val spoken = status == TextToSpeech.SUCCESS &&
            engine.setLanguage(speechLocale()) >= TextToSpeech.LANG_AVAILABLE
        if (!spoken) {
            Log.w(TAG, "$language speech isn't available (status $status)")
            unavailable = true
            return@TextToSpeech
        }
        engine.setAudioAttributes(attributes)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) = releaseFocusWhenQuiet()
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = releaseFocusWhenQuiet()
        })
        ready = true
        pending?.let { pending = null; speak(it) }
    }.also {
        // Read again for each run: the app's language may have changed since the last one
        language = VoiceLanguage.of(AppLocale.language())
        unavailable = false
        tts = it
    }

    /** English in the phone's own variety (en-GB, en-US...) when the app is in English. */
    private fun speechLocale(): Locale = when (language) {
        VoiceLanguage.HEBREW -> Locale.forLanguageTag(VoiceLanguage.HEBREW.tag)
        VoiceLanguage.ENGLISH -> AppLocale.current().takeIf { it.language == "en" } ?: Locale.forLanguageTag(VoiceLanguage.ENGLISH.tag)
    }

    private fun releaseFocusWhenQuiet() {
        if (tts?.isSpeaking != true) audioManager.abandonAudioFocusRequest(focus)
    }

    private companion object {
        const val TAG = "RunVoice"
    }
}
