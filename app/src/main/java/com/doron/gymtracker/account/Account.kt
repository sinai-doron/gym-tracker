package com.doron.gymtracker.account

import android.app.Activity
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The account as the screens see it: sign in after consent, change the routes switch, sign out.
 * Owns the order of things, so no screen can sign someone in without their consent on record.
 */
@Singleton
class Account @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: AuthRepository,
    private val consent: SyncConsent,
    private val api: BackendApi,
    private val owner: DataOwner,
) {
    val user = auth.user
    val consentState = consent.consent

    /**
     * Called from the consent screen after the user agreed. The consent is recorded first, so a
     * session never exists here without it; if sign-in then fails or is cancelled, it is cleared.
     */
    suspend fun signIn(activity: Activity, routes: Boolean, music: Boolean): SignedInUser {
        consent.accept(routes, music)
        val user = try {
            auth.signIn(deviceLabel()) { nonce -> GoogleIdTokens(activity).account(nonce) }
        } catch (e: Exception) {
            consent.clear()
            throw e
        }
        // Another person's account: out again before anything syncs, and nothing else changes.
        val holder = owner.current()
        if (!DataOwner.allows(holder, user)) {
            signOut()
            throw DifferentAccount(holder!!.email)
        }
        owner.claim(user)
        uploadConsent()
        return user
    }

    suspend fun setRoutes(routes: Boolean) {
        consent.setRoutes(routes)
        uploadConsent()
    }

    suspend fun setMusic(music: Boolean) {
        consent.setMusic(music)
        uploadConsent()
    }

    /**
     * Sends the consent to the backend when it hasn't seen it yet. Failure is fine: sync tries
     * again before every push, and the backend refuses to store anything until it has it.
     */
    suspend fun uploadConsent(): Boolean {
        val current = consent.current() ?: return false
        if (current.uploaded) return true
        if (auth.currentUser() == null) return false
        return runCatching {
            auth.withAccessToken { token ->
                api.put<ConsentBody, ConsentResponse>(
                    "/api/gym/consent", ConsentBody(current.version, current.routes, current.music.takeIf { it }), token,
                )
            }
            consent.markUploaded(current.acceptedAt)
        }.isSuccess
    }

    /**
     * Whether the signed-in user may sync this phone's data, claiming it if nobody has yet (the
     * first version with this check, on a phone already signed in, claims it for that account).
     */
    suspend fun mayReadAndWriteThisPhonesData(): Boolean {
        val user = auth.currentUser() ?: return false
        val holder = owner.current() ?: return owner.claim(user).userId == user.id
        return DataOwner.allows(holder, user)
    }

    /** Ends the session and forgets the consent. The workouts on this phone stay. */
    suspend fun signOut() {
        auth.signOut()
        consent.clear()
        GoogleIdTokens.forget(context)
    }

    private fun deviceLabel(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(100)

    @Serializable
    // `music` only when on: a backend from before it refuses the field, and absent means no.
    private data class ConsentBody(val version: String, val routes: Boolean, val music: Boolean? = null)

    @Serializable
    private data class ConsentResponse(val consent: Map<String, kotlinx.serialization.json.JsonElement>)
}
