package com.doron.gymtracker.account

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.doron.gymtracker.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** What Google's account picker hands back: the ID token, and the person's name and picture. */
data class GoogleAccount(val idToken: String, val displayName: String?, val pictureUrl: String?)

/**
 * Google ID tokens from Credential Manager, minted for the backend's web client and bound to the
 * backend's challenge nonce (doron-backend auth design §6.1). The legacy Google Sign-In library is
 * not used.
 */
class GoogleIdTokens(private val activity: Activity) {

    private val credentials = CredentialManager.create(activity)

    /** Shows Google's account picker. Throws [SignInCancelled] when the user backs out. */
    suspend fun account(nonce: String): GoogleAccount {
        val option = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .setNonce(nonce)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = try {
            credentials.getCredential(activity, request)
        } catch (e: GetCredentialCancellationException) {
            throw SignInCancelled()
        } catch (e: NoCredentialException) {
            throw SignInFailed("No Google account on this phone", e)
        } catch (e: GetCredentialException) {
            throw SignInFailed("Google sign-in failed", e)
        }
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw SignInFailed("Unexpected credential from Google", null)
        }
        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return GoogleAccount(google.idToken, google.displayName, google.profilePictureUri?.toString())
    }

    companion object {
        /** After signing out, so Google doesn't silently pick the same account next time. */
        suspend fun forget(context: Context) {
            runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
        }
    }
}

class SignInCancelled : Exception("cancelled")
class SignInFailed(message: String, cause: Throwable?) : Exception(message, cause)
