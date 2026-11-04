package com.doron.gymtracker.account

import com.doron.gymtracker.data.prefs.SettingsPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Google sign-in and sync, behind a flag until they are ready for everyone: off, the welcome
 * screens skip the account page and Settings has no Account section, so nothing signs in and
 * nothing syncs. It is on when this phone is already signed in (that account keeps syncing and can
 * still sign out), or when switched on by hand: seven taps on the version line in Settings.
 */
@Singleton
class SignInFeature @Inject constructor(
    private val prefs: SettingsPrefs,
    private val account: Account
) {
    val enabled: Flow<Boolean> = enabled(prefs.signInSwitchedOn, account.user)

    /** Flips the hand switch; returns whether sign-in is now on. */
    suspend fun toggle(): Boolean {
        val on = !prefs.signInSwitchedOn.first()
        prefs.setSignInSwitchedOn(on)
        return enabled.first()
    }

    companion object {
        internal fun enabled(switchedOn: Flow<Boolean>, user: Flow<Any?>): Flow<Boolean> =
            combine(switchedOn, user) { on, signedIn -> on || signedIn != null }
    }
}
