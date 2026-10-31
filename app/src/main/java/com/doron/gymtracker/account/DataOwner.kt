package com.doron.gymtracker.account

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataOwnerStore: DataStore<Preferences> by preferencesDataStore(name = "data_owner")
private val USER_ID = stringPreferencesKey("user_id")
private val EMAIL = stringPreferencesKey("email")

/** The account whose workouts this phone holds. */
data class Owner(val userId: String, val email: String)

/**
 * One person's data per phone. The first account that signs in owns the workouts here; another
 * account signing in later would otherwise sync them into its own account, so it is refused.
 * Signing out keeps the owner (the data is still theirs). Clearing the app's data, or a fresh
 * install, starts over. The file sits with the app's other settings, so the backup zip carries it
 * and a restored phone still knows whose data it is.
 */
@Singleton
class DataOwner @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun current(): Owner? = context.dataOwnerStore.data.map { p ->
        val id = p[USER_ID] ?: return@map null
        Owner(id, p[EMAIL] ?: "")
    }.first()

    /** Records [user] as the owner when there is none yet; returns the owner either way. */
    suspend fun claim(user: SignedInUser): Owner {
        context.dataOwnerStore.edit { if (it[USER_ID] == null) { it[USER_ID] = user.id; it[EMAIL] = user.email } }
        return current()!!
    }

    companion object {
        /** Whether [user] may sync this phone's data. No owner yet means whoever signs in claims it. */
        fun allows(owner: Owner?, user: SignedInUser): Boolean = owner == null || owner.userId == user.id
    }
}

/** Another account than the one whose workouts are on this phone tried to sign in. */
class DifferentAccount(val ownerEmail: String) : Exception("this phone's data belongs to another account")
