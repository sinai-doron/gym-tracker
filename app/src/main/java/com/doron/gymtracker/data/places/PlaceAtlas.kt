package com.doron.gymtracker.data.places

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

/** A place a run can earn a badge in: a country, or a state of one. */
data class Place(
    val id: String,
    val kind: Kind,
    /** The country a state is in; null for a country. */
    val country: String?,
    /** Its name by language: "en", "he". */
    val names: Map<String, String>,
    /** Its outline, rings of (longitude, latitude). */
    val outline: List<List<DoubleArray>>
) {
    enum class Kind { COUNTRY, STATE }

    fun nameIn(language: String): String = names[language] ?: names.getValue("en")

    private val bounds = outline.flatten().let { pts ->
        doubleArrayOf(pts.minOf { it[0] }, pts.minOf { it[1] }, pts.maxOf { it[0] }, pts.maxOf { it[1] })
    }

    /** Whether the point is inside: in any of its rings, by the even-odd rule. */
    fun contains(latitude: Double, longitude: Double): Boolean {
        if (longitude < bounds[0] || latitude < bounds[1] || longitude > bounds[2] || latitude > bounds[3]) return false
        return outline.any { ring -> inRing(ring, longitude, latitude) }
    }

    private fun inRing(ring: List<DoubleArray>, x: Double, y: Double): Boolean {
        var inside = false
        var j = ring.lastIndex
        for (i in ring.indices) {
            val (xi, yi) = ring[i][0] to ring[i][1]
            val (xj, yj) = ring[j][0] to ring[j][1]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }
}

/**
 * The places runs can earn badges in, from the asset scripts/make_place_badges.py writes: the
 * USA with its 50 states, Israel and the UK. Read on first use; finding a run's place needs no
 * network.
 */
@Singleton
class PlaceAtlas @Inject constructor(@ApplicationContext private val context: Context) {

    private val lock = Mutex()
    private var places: List<Place>? = null

    suspend fun all(): List<Place> = lock.withLock {
        places ?: withContext(Dispatchers.IO) {
            parse(context.assets.open(ASSET).bufferedReader().use { it.readText() })
        }.also { places = it }
    }

    /** The country and, where there is one, the state the point is in; empty outside them all. */
    suspend fun locate(latitude: Double, longitude: Double): List<Place> = locate(all(), latitude, longitude)

    @Serializable
    private data class File(val places: List<Item>)

    @Serializable
    private data class Item(
        val id: String,
        val kind: String,
        val country: String? = null,
        val names: Map<String, String>,
        val outline: List<List<List<Double>>>
    )

    companion object {
        const val ASSET = "places/places.json"
        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): List<Place> = json.decodeFromString(File.serializer(), text).places.map { p ->
            Place(
                id = p.id,
                kind = if (p.kind == "state") Place.Kind.STATE else Place.Kind.COUNTRY,
                country = p.country,
                names = p.names,
                outline = p.outline.map { ring -> ring.map { doubleArrayOf(it[0], it[1]) } }
            )
        }

        /** A state counts its country too, should the country's coarser outline miss the point. */
        fun locate(places: List<Place>, latitude: Double, longitude: Double): List<Place> {
            val states = places.filter { it.kind == Place.Kind.STATE && it.contains(latitude, longitude) }.take(1)
            val countryIds = states.mapNotNull { it.country }.toSet()
            val countries = places.filter {
                it.kind == Place.Kind.COUNTRY && (it.id in countryIds || it.contains(latitude, longitude))
            }.take(1)
            return countries + states
        }
    }
}
