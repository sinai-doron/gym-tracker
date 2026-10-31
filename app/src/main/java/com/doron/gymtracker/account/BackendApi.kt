package com.doron.gymtracker.account

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * doron-backend's HTTP API: mobile sign-in (its auth design §6) and gym-tracker's sync.
 *
 * Tokens travel only in JSON bodies and the Authorization header, never in a URL, and nothing
 * here logs a token or workout data. Ported from the receipts app (sinai-doron/myR).
 */
class BackendApi(
    baseUrl: String,
    private val client: OkHttpClient = defaultClient(),
) {
    private val base = baseUrl.trimEnd('/')

    suspend fun challenge(): Challenge =
        post("/api/auth/mobile/challenge", ChallengeRequest(platform = PLATFORM), accessToken = null)

    suspend fun signInWithGoogle(challengeId: String, idToken: String, deviceLabel: String): SignInResponse =
        post(
            "/api/auth/mobile/google",
            SignInRequest(challengeId, idToken, Device(platform = PLATFORM, label = deviceLabel)),
            accessToken = null,
        )

    suspend fun refresh(refreshToken: String): IssuedTokens =
        post("/api/auth/token/refresh", RefreshRequest(refreshToken), accessToken = null)

    suspend fun logout(accessToken: String) {
        call(request("/api/auth/logout", accessToken).post(EMPTY_BODY).build()) { }
    }

    internal suspend inline fun <reified Req, reified Res> post(path: String, body: Req, accessToken: String?): Res {
        val request = request(path, accessToken)
            .post(json.encodeToString(body).toRequestBody(JSON))
            .build()
        return call(request) { response -> json.decodeFromString<Res>(response.body!!.string()) }
    }

    internal suspend inline fun <reified Req, reified Res> put(path: String, body: Req, accessToken: String): Res {
        val request = request(path, accessToken)
            .put(json.encodeToString(body).toRequestBody(JSON))
            .build()
        return call(request) { response -> json.decodeFromString<Res>(response.body!!.string()) }
    }

    internal suspend inline fun <reified Res> get(path: String, accessToken: String): Res =
        call(request(path, accessToken).get().build()) { response ->
            json.decodeFromString<Res>(response.body!!.string())
        }

    internal suspend fun delete(path: String, accessToken: String) {
        call(request(path, accessToken).delete().build()) { }
    }

    internal fun request(path: String, accessToken: String?): Request.Builder =
        Request.Builder().url(base + path).apply {
            if (accessToken != null) header("Authorization", "Bearer $accessToken")
        }

    internal suspend fun <T> call(request: Request, onSuccess: (Response) -> T): T =
        withContext(Dispatchers.IO) {
            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                throw BackendException.Network(e)
            }
            response.use {
                when {
                    it.isSuccessful -> onSuccess(it)
                    it.code == 401 -> throw BackendException.Unauthorized
                    it.code == 403 -> throw BackendException.NotAllowed
                    it.code == 409 -> throw BackendException.Conflict
                    it.code == 429 -> throw BackendException.RateLimited(it.header("Retry-After")?.toLongOrNull() ?: 60)
                    else -> throw BackendException.Http(it.code)
                }
            }
        }

    @Serializable
    private data class ChallengeRequest(val platform: String)

    @Serializable
    data class Challenge(val challengeId: String, val nonce: String)

    @Serializable
    private data class Device(val platform: String, val label: String)

    @Serializable
    private data class SignInRequest(val challengeId: String, val idToken: String, val device: Device)

    @Serializable
    private data class RefreshRequest(val refreshToken: String)

    @Serializable
    data class User(val id: String, val email: String, val displayName: String? = null)

    @Serializable
    data class IssuedTokens(val accessToken: String, val accessTokenExpiresIn: Long, val refreshToken: String)

    @Serializable
    data class SignInResponse(
        val accessToken: String,
        val accessTokenExpiresIn: Long,
        val refreshToken: String,
        val user: User,
    )

    companion object {
        private const val PLATFORM = "android"
        internal val JSON = "application/json".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        internal val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}

sealed class BackendException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** No valid session: the access or refresh token was refused. */
    data object Unauthorized : BackendException("unauthorized") {
        private fun readResolve(): Any = Unauthorized
    }

    /** Google sign-in worked but this account is not on the backend's allowlist. */
    data object NotAllowed : BackendException("not allowed") {
        private fun readResolve(): Any = NotAllowed
    }

    /** The backend has no consent for this user yet (sync). */
    data object Conflict : BackendException("conflict") {
        private fun readResolve(): Any = Conflict
    }

    class RateLimited(val retryAfterSeconds: Long) : BackendException("rate limited")
    class Http(val code: Int) : BackendException("HTTP $code")
    class Network(cause: IOException) : BackendException("network: ${cause.message}", cause)
}
