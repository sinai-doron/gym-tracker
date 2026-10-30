package com.doron.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "app_settings")

private val LANGUAGE_CODE = stringPreferencesKey("language_code")
private val MEDIA_PERMISSION_DISMISSED = booleanPreferencesKey("media_permission_dismissed")
private val RUN_TARGET_TYPE = stringPreferencesKey("run_target_type")
private val RUN_TARGET_DISTANCE_KM = floatPreferencesKey("run_target_distance_km")
private val RUN_TARGET_DURATION_MIN = intPreferencesKey("run_target_duration_min")
private val LAP_DISTANCE_M = intPreferencesKey("lap_distance_m")
private val STEP_GOAL = intPreferencesKey("step_goal")
private val BODY_WEIGHT_KG = floatPreferencesKey("body_weight_kg")
private val AUDIO_FEEDBACK = stringPreferencesKey("audio_feedback")
private val RUN_TARGET_ROUTE_ID = longPreferencesKey("run_target_route_id")

enum class AudioFeedback { VOICE, TONES, OFF }

@Singleton
class SettingsPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val languageCode: Flow<String> = context.settingsDataStore.data
        .map { prefs -> prefs[LANGUAGE_CODE] ?: "iw" }  // default Hebrew

    suspend fun setLanguageCode(code: String) {
        context.settingsDataStore.edit { prefs -> prefs[LANGUAGE_CODE] = code }
    }

    val mediaPermissionDismissed: Flow<Boolean> = context.settingsDataStore.data
        .map { prefs -> prefs[MEDIA_PERMISSION_DISMISSED] ?: false }

    suspend fun dismissMediaPermission() {
        context.settingsDataStore.edit { prefs -> prefs[MEDIA_PERMISSION_DISMISSED] = true }
    }

    /** The last run target chosen, so the next run starts with it. */
    val runTarget: Flow<RunTarget> = context.settingsDataStore.data.map { prefs ->
        RunTarget(
            type = prefs[RUN_TARGET_TYPE]?.let { runCatching { RunTargetType.valueOf(it) }.getOrNull() } ?: RunTargetType.NONE,
            distanceKm = prefs[RUN_TARGET_DISTANCE_KM] ?: RunTarget.DEFAULT_DISTANCE_KM,
            durationMin = prefs[RUN_TARGET_DURATION_MIN] ?: RunTarget.DEFAULT_DURATION_MIN,
            routeId = prefs[RUN_TARGET_ROUTE_ID]
        )
    }

    suspend fun setRunTarget(target: RunTarget) {
        context.settingsDataStore.edit { prefs ->
            prefs[RUN_TARGET_TYPE] = target.type.name
            prefs[RUN_TARGET_DISTANCE_KM] = target.distanceKm
            prefs[RUN_TARGET_DURATION_MIN] = target.durationMin
            target.routeId?.let { prefs[RUN_TARGET_ROUTE_ID] = it } ?: prefs.remove(RUN_TARGET_ROUTE_ID)
        }
    }

    /** Lap length for runs, in meters; 0 turns laps off. */
    val lapDistanceM: Flow<Int> = context.settingsDataStore.data
        .map { prefs -> prefs[LAP_DISTANCE_M] ?: DEFAULT_LAP_DISTANCE_M }

    suspend fun setLapDistanceM(meters: Int) {
        context.settingsDataStore.edit { prefs -> prefs[LAP_DISTANCE_M] = meters }
    }

    /** Steps to aim for each day. */
    val stepGoal: Flow<Int> = context.settingsDataStore.data
        .map { prefs -> prefs[STEP_GOAL] ?: DEFAULT_STEP_GOAL }

    suspend fun setStepGoal(steps: Int) {
        context.settingsDataStore.edit { prefs -> prefs[STEP_GOAL] = steps }
    }

    /** For calories; null until set. */
    val bodyWeightKg: Flow<Float?> = context.settingsDataStore.data.map { prefs -> prefs[BODY_WEIGHT_KG] }

    suspend fun setBodyWeightKg(kg: Float?) {
        context.settingsDataStore.edit { prefs ->
            if (kg == null) prefs.remove(BODY_WEIGHT_KG) else prefs[BODY_WEIGHT_KG] = kg
        }
    }

    /** How runs speak up for laps, the target and auto-pause. */
    val audioFeedback: Flow<AudioFeedback> = context.settingsDataStore.data.map { prefs ->
        prefs[AUDIO_FEEDBACK]?.let { runCatching { AudioFeedback.valueOf(it) }.getOrNull() } ?: AudioFeedback.VOICE
    }

    suspend fun setAudioFeedback(feedback: AudioFeedback) {
        context.settingsDataStore.edit { prefs -> prefs[AUDIO_FEEDBACK] = feedback.name }
    }

    companion object {
        const val DEFAULT_LAP_DISTANCE_M = 1000
        val LAP_DISTANCE_OPTIONS_M = listOf(0, 200, 400, 500, 1000, 2000, 5000)
        const val DEFAULT_STEP_GOAL = 10_000
        val STEP_GOAL_OPTIONS = listOf(5_000, 6_000, 7_000, 8_000, 10_000, 12_000, 15_000, 20_000)
    }
}
