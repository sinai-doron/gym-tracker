package com.doron.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "app_settings")

private val LANGUAGE_CODE = stringPreferencesKey("language_code")

@Singleton
class SettingsPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val languageCode: Flow<String> = context.settingsDataStore.data
        .map { prefs -> prefs[LANGUAGE_CODE] ?: "iw" }  // default Hebrew

    suspend fun setLanguageCode(code: String) {
        context.settingsDataStore.edit { prefs -> prefs[LANGUAGE_CODE] = code }
    }
}
