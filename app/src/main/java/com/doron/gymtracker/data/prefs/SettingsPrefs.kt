package com.doron.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
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
            durationMin = prefs[RUN_TARGET_DURATION_MIN] ?: RunTarget.DEFAULT_DURATION_MIN
        )
    }

    suspend fun setRunTarget(target: RunTarget) {
        context.settingsDataStore.edit { prefs ->
            prefs[RUN_TARGET_TYPE] = target.type.name
            prefs[RUN_TARGET_DISTANCE_KM] = target.distanceKm
            prefs[RUN_TARGET_DURATION_MIN] = target.durationMin
        }
    }
}
