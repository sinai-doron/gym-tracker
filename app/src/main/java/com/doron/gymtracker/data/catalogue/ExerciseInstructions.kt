package com.doron.gymtracker.data.catalogue

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * How to do each built-in exercise, step by step, from the shipped catalogue: wger's English
 * text and our Hebrew translation of it. Read from the asset on first use, not kept in the
 * database: it is only shown, and an app update brings new text with the asset.
 */
@Singleton
class ExerciseInstructions @Inject constructor(@ApplicationContext private val context: Context) {

    private val lock = Mutex()
    private var byId: Map<String, Map<String, List<String>>>? = null

    /**
     * The steps in each language they're written in; empty for an exercise of the user's own or
     * one with none. All of them, so the screen can follow a change of language ([pick]).
     */
    suspend fun byLanguage(libraryExerciseId: String): Map<String, List<String>> =
        load()[libraryExerciseId].orEmpty()

    private suspend fun load(): Map<String, Map<String, List<String>>> = lock.withLock {
        byId ?: withContext(Dispatchers.IO) {
            parse(context.assets.open(ExerciseCatalogue.ASSET).bufferedReader().use { it.readText() })
        }.also { byId = it }
    }

    @Serializable
    private data class File(val exercises: List<Item>)

    @Serializable
    private data class Item(val slug: String, val instructions: Map<String, List<String>> = emptyMap())

    companion object {
        private const val FALLBACK = "en"
        private val json = Json { ignoreUnknownKeys = true }

        /** The steps in [language], else in English. */
        fun pick(byLanguage: Map<String, List<String>>, language: String): List<String> =
            byLanguage[language]?.takeIf { it.isNotEmpty() } ?: byLanguage[FALLBACK].orEmpty()

        /** Library exercise id -> language -> steps, for the exercises that have any. */
        fun parse(text: String): Map<String, Map<String, List<String>>> =
            json.decodeFromString(File.serializer(), text).exercises
                .filter { it.instructions.isNotEmpty() }
                .associate { ExerciseCatalogue.builtinId(it.slug) to it.instructions }
    }
}
