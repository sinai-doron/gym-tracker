package com.doron.gymtracker.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncConsentStore: DataStore<Preferences> by preferencesDataStore(name = "sync_consent")

private val VERSION = stringPreferencesKey("version")
private val ROUTES = booleanPreferencesKey("routes")
private val MUSIC = booleanPreferencesKey("music")
private val ACCEPTED_AT = longPreferencesKey("accepted_at")
private val UPLOADED = booleanPreferencesKey("uploaded")

/**
 * What the signed-in user agreed to on the consent screen: the version of its text, when, and
 * whether run routes (GPS) and the music log may sync too, each its own choice. Signing in alone is not consent; nothing syncs without
 * this. The backend keeps its own copy (PUT /api/gym/consent) and enforces it there as well.
 */
data class Consent(
    val version: String,
    val routes: Boolean,
    val music: Boolean,
    val acceptedAt: Long,
    val uploaded: Boolean,
)

@Singleton
class SyncConsent @Inject constructor(@ApplicationContext private val context: Context) {

    val consent: Flow<Consent?> = context.syncConsentStore.data.map { p ->
        val version = p[VERSION] ?: return@map null
        Consent(version, p[ROUTES] ?: false, p[MUSIC] ?: false, p[ACCEPTED_AT] ?: 0, p[UPLOADED] ?: false)
    }

    suspend fun current(): Consent? = consent.first()

    /** The user agreed to the current text. Not yet on the backend until [markUploaded]. */
    suspend fun accept(routes: Boolean, music: Boolean, now: Long = System.currentTimeMillis()) {
        context.syncConsentStore.edit {
            it[VERSION] = CURRENT_VERSION
            it[ROUTES] = routes
            it[MUSIC] = music
            it[ACCEPTED_AT] = now
            it[UPLOADED] = false
        }
    }

    /** The routes switch, changed later in Settings: a new consent for the backend. */
    suspend fun setRoutes(routes: Boolean) {
        context.syncConsentStore.edit {
            if (it[VERSION] == null) return@edit
            it[ROUTES] = routes
            it[ACCEPTED_AT] = System.currentTimeMillis()
            it[UPLOADED] = false
        }
    }

    /** The music switch, changed later in Settings: a new consent for the backend. */
    suspend fun setMusic(music: Boolean) {
        context.syncConsentStore.edit {
            if (it[VERSION] == null) return@edit
            it[MUSIC] = music
            it[ACCEPTED_AT] = System.currentTimeMillis()
            it[UPLOADED] = false
        }
    }

    suspend fun markUploaded(acceptedAt: Long) {
        context.syncConsentStore.edit { if (it[ACCEPTED_AT] == acceptedAt) it[UPLOADED] = true }
    }

    suspend fun clear() {
        context.syncConsentStore.edit { it.clear() }
    }

    companion object {
        /** Bump when the consent screen's text changes in substance; users are then asked again. */
        // .2: the music log became its own choice. A consent to the first text stays valid, with
        // music off, since that text did not offer it.
        const val CURRENT_VERSION = "2026-10-31.2"
    }
}
