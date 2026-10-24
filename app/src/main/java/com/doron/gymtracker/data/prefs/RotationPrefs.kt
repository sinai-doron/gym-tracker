package com.doron.gymtracker.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doron.gymtracker.domain.model.WorkoutLabel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gym_prefs")
private val LAST_WORKOUT_LABEL = stringPreferencesKey("last_workout_label")

@Singleton
class RotationPrefs @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun getLastLabel(): WorkoutLabel? =
        context.dataStore.data.map { prefs ->
            prefs[LAST_WORKOUT_LABEL]?.let { WorkoutLabel.valueOf(it) }
        }.first()

    suspend fun setLastLabel(label: WorkoutLabel) {
        context.dataStore.edit { prefs -> prefs[LAST_WORKOUT_LABEL] = label.name }
    }
}
