package com.doron.gymtracker.account

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

/**
 * The client half of doron-backend's mobile auth (its auth design §6), against a fake backend that
 * behaves like the real one: refresh tokens rotate, and a used one revokes the session. Ported
 * from the receipts app, which signs in the same way.
 */
class AuthRepositoryTest {

    private val server = MockWebServer()
    private val backend = FakeBackend()
    private val store = MemorySessionStore()
    private var clock = 1_000_000L
    private lateinit var auth: AuthRepository
    private lateinit var api: BackendApi

    @Before
    fun setUp() {
        server.dispatcher = backend
        server.start()
        api = BackendApi(server.url("/").toString())
        auth = AuthRepository(api, store, now = { clock })
    }

    @After
    fun tearDown() = server.shutdown()

    private suspend fun signIn(): SignedInUser =
        auth.signIn(deviceLabel = "Pixel 10") { nonce ->
            assertEquals("nonce-1", nonce)
            GoogleAccount("google-id-token", "Doron G", "https://lh3.googleusercontent.com/a/photo")
        }

    @Test
    fun `sign-in sends the challenge id and ID token, and stores only the refresh token and the profile`() = runBlocking {
        val user = signIn()

        assertEquals("doron@example.com", user.email)
        assertEquals("https://lh3.googleusercontent.com/a/photo", user.pictureUrl)
        assertEquals(
            StoredSession("refresh-1", "user-1", "doron@example.com", "Doron", "https://lh3.googleusercontent.com/a/photo"),
            store.current(),
        )
        val body = backend.bodies.first { it.contains("idToken") }
        assertTrue(body.contains("\"challengeId\":\"challenge-1\""))
        assertTrue(body.contains("\"idToken\":\"google-id-token\""))
        assertTrue(body.contains("\"platform\":\"android\""))
        assertTrue(body.contains("\"label\":\"Pixel 10\""))
        assertFalse("the access token is never stored", store.current().toString().contains("access"))
    }

    @Test
    fun `the backend's display name wins, and Google's fills in when the backend has none`() = runBlocking {
        backend.displayName = null
        val user = signIn()
        assertEquals("Doron G", user.displayName)
    }

    @Test
    fun `a fresh access token is used as is`() = runBlocking {
        signIn()
        assertEquals("access-1", auth.withAccessToken { it })
        assertEquals(0, backend.refreshes.get())
    }

    @Test
    fun `an access token near expiry is refreshed, and the new refresh token replaces the old`() = runBlocking {
        signIn()
        clock += 15 * 60_000L
        assertEquals("access-2", auth.withAccessToken { it })
        assertEquals("refresh-2", store.current()?.refreshToken)
    }

    @Test
    fun `concurrent calls share one refresh, so a token is never presented twice`() = runBlocking {
        signIn()
        clock += 15 * 60_000L

        val tokens = (1..8).map { async { auth.withAccessToken { it } } }.awaitAll()

        assertEquals(1, backend.refreshes.get())
        assertEquals(setOf("access-2"), tokens.toSet())
        assertFalse(backend.reuseDetected)
    }

    @Test
    fun `after a restart the stored refresh token gets a new access token`() = runBlocking {
        signIn()
        val restarted = AuthRepository(api, store, now = { clock })
        assertEquals("access-2", restarted.withAccessToken { it })
    }

    @Test
    fun `a refused refresh token signs the app out`() = runBlocking {
        signIn()
        backend.revokeAll = true
        clock += 15 * 60_000L
        try {
            auth.withAccessToken { it }
            fail("expected Unauthorized")
        } catch (e: BackendException.Unauthorized) {
            // expected
        }
        assertNull(store.current())
        assertNull(auth.currentUser())
    }

    @Test
    fun `a 401 on a call refreshes once and retries`() = runBlocking {
        signIn()
        var attempts = 0
        val result = auth.withAccessToken { token ->
            attempts++
            if (token == "access-1") throw BackendException.Unauthorized
            "ok with $token"
        }
        assertEquals("ok with access-2", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `a Google account off the allowlist is refused and nothing is stored`() = runBlocking {
        backend.allowlisted = false
        try {
            signIn()
            fail("expected NotAllowed")
        } catch (e: BackendException) {
            assertEquals(BackendException.NotAllowed, e)
        }
        assertNull(store.current())
    }

    @Test
    fun `sign-out forgets the session and revokes it on the backend`() = runBlocking {
        signIn()
        auth.signOut()
        assertNull(store.current())
        assertTrue(backend.loggedOutWith.contains("Bearer access-1"))
    }

    /** Just enough of doron-backend's mobile auth. */
    private class FakeBackend : Dispatcher() {
        val bodies: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val loggedOutWith: MutableList<String?> = Collections.synchronizedList(mutableListOf())
        val refreshes = AtomicInteger()
        var allowlisted = true
        var revokeAll = false
        var displayName: String? = "Doron"

        @Volatile
        var reuseDetected = false
        private var generation = 1
        private val usedRefreshTokens = mutableSetOf<String>()

        @Synchronized
        override fun dispatch(request: RecordedRequest): MockResponse {
            val body = request.body.readUtf8()
            bodies += body
            return when (request.requestUrl?.encodedPath) {
                "/api/auth/mobile/challenge" -> json("""{"challengeId":"challenge-1","nonce":"nonce-1"}""")
                "/api/auth/mobile/google" ->
                    if (!allowlisted) json("""{"error":"not_authorized"}""", 403)
                    else json(
                        """{"accessToken":"access-1","accessTokenExpiresIn":900,"refreshToken":"refresh-1",
                           "user":{"id":"user-1","email":"doron@example.com","displayName":${displayName?.let { "\"$it\"" } ?: "null"}}}""",
                    )
                "/api/auth/token/refresh" -> {
                    refreshes.incrementAndGet()
                    val presented = Regex("\"refreshToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
                    when {
                        revokeAll -> json("{}", 401)
                        presented in usedRefreshTokens || presented != "refresh-$generation" -> {
                            reuseDetected = true
                            revokeAll = true
                            json("{}", 401)
                        }
                        else -> {
                            usedRefreshTokens += presented
                            generation++
                            json("""{"accessToken":"access-$generation","accessTokenExpiresIn":900,"refreshToken":"refresh-$generation"}""")
                        }
                    }
                }
                "/api/auth/logout" -> {
                    loggedOutWith += request.getHeader("Authorization")
                    MockResponse().setResponseCode(204)
                }
                else -> MockResponse().setResponseCode(404)
            }
        }

        private fun json(body: String, code: Int = 200) = MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body)
    }

    private class MemorySessionStore : SessionStore {
        private val state = MutableStateFlow<StoredSession?>(null)
        override val session: StateFlow<StoredSession?> = state
        override suspend fun current(): StoredSession? = state.value
        override suspend fun save(session: StoredSession) { state.value = session }
        override suspend fun clear() { state.value = null }
    }
}
