package com.doron.gymtracker.account

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.GeneralSecurityException

/** What survives an app restart: the refresh token and who it belongs to. */
@Serializable
data class StoredSession(
    val refreshToken: String,
    val userId: String,
    val email: String,
    val displayName: String? = null,
    val pictureUrl: String? = null,
)

/** Where the signed-in session is kept. The access token is never stored (auth design §6.5). */
interface SessionStore {
    val session: Flow<StoredSession?>
    suspend fun current(): StoredSession?
    suspend fun save(session: StoredSession)
    suspend fun clear()
}

/**
 * The session in one file, encrypted with Tink AES-256-GCM under a key held in the Android
 * Keystore (auth design §6.5). The file and the keyset are excluded from backup and device
 * transfer (res/xml/data_extraction_rules.xml, backup_rules.xml), so a restored phone starts
 * signed out. Tink is used directly because datastore-tink needs a newer toolchain than this app's.
 */
class EncryptedSessionStore(context: Context) : SessionStore {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val aead: Aead by lazy { aead(context.applicationContext) }
    private val lock = Mutex()
    private val state = MutableStateFlow<StoredSession?>(null)
    @Volatile private var loaded = false

    // Reads the file on first subscription, so a fresh start never shows "signed out" by mistake.
    override val session: Flow<StoredSession?> = flow {
        load()
        emitAll(state.asStateFlow())
    }

    override suspend fun current(): StoredSession? = load()

    override suspend fun save(session: StoredSession) = lock.withLock {
        withContext(Dispatchers.IO) {
            val plain = json.encodeToString(StoredSession.serializer(), session).toByteArray()
            val partial = File(file.parentFile, "$FILE_NAME.part")
            partial.writeBytes(aead.encrypt(plain, FILE_NAME.toByteArray()))
            if (!partial.renameTo(file)) error("could not save the session")
        }
        state.value = session
        loaded = true
    }

    override suspend fun clear() = lock.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        state.value = null
        loaded = true
    }

    /** Reads the file once; a file that can't be decrypted (a new Keystore key, say) means signed out. */
    private suspend fun load(): StoredSession? {
        if (loaded) return state.value
        return lock.withLock {
            if (!loaded) {
                state.value = withContext(Dispatchers.IO) {
                    if (!file.exists()) return@withContext null
                    try {
                        val plain = aead.decrypt(file.readBytes(), FILE_NAME.toByteArray())
                        json.decodeFromString(StoredSession.serializer(), plain.decodeToString())
                    } catch (e: GeneralSecurityException) {
                        file.delete(); null
                    } catch (e: IllegalArgumentException) {
                        file.delete(); null
                    }
                }
                loaded = true
            }
            state.value
        }
    }

    private companion object {
        const val FILE_NAME = "session.enc"
        const val KEYSET_NAME = "session_keyset"
        const val KEYSET_PREFS = "session_keyset_prefs"
        const val MASTER_KEY_URI = "android-keystore://gymtracker_session_key"
        val json = Json { ignoreUnknownKeys = true }

        fun aead(context: Context): Aead {
            AeadConfig.register()
            return AndroidKeysetManager.Builder()
                .withSharedPref(context, KEYSET_NAME, KEYSET_PREFS)
                .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                .withMasterKeyUri(MASTER_KEY_URI)
                .build()
                .keysetHandle
                .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
        }
    }
}
