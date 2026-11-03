package com.doron.gymtracker.data.catalogue

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The front and back figures the exercise picker draws (assets/body_map/body_map.json, written by
 * scripts/build_body_map.py from react-native-body-highlighter, MIT). Paths are SVG path data in
 * the view's own box; the back figure's box starts at x 724.
 */
@Serializable
data class BodyMap(val source: BodyMapSource, val views: Map<String, BodyMapView>) {

    companion object {
        const val ASSET = "body_map/body_map.json"
        const val LICENSE_ASSET = "body_map/LICENSE.txt"
        const val FRONT = "front"
        const val BACK = "back"

        private val json = Json { ignoreUnknownKeys = true }

        fun parse(text: String): BodyMap = json.decodeFromString(serializer(), text)
    }
}

@Serializable
data class BodyMapSource(val name: String, val version: String, val license: String, val url: String)

@Serializable
data class BodyMapView(val viewBox: List<Float>, val outline: String, val parts: List<BodyMapPart>)

@Serializable
data class BodyMapPart(val slug: String, val paths: List<String>)

/** What a tap on the body map means in the 18 muscles of LibraryVocabulary. */
object BodyRegions {

    /** The muscles [region] stands for on the [view] figure: the front shoulder is front and side delts. */
    fun muscles(region: String, view: String): List<String> =
        LibraryVocabulary.MUSCLE_REGIONS.filterValues { it.region == region && (it.view == view || it.view == BOTH) }.keys.toList()

    /** A tap adds the region's muscles, or takes them all away when they were all chosen already. */
    fun toggle(selected: Set<String>, muscles: List<String>): Set<String> =
        if (selected.containsAll(muscles)) selected - muscles.toSet() else selected + muscles

    /** The regions of [view] to draw as chosen: those with any of their muscles in [selected]. */
    fun highlighted(selected: Set<String>, view: String): Set<String> =
        LibraryVocabulary.MUSCLE_REGIONS.filter { (muscle, r) -> muscle in selected && (r.view == view || r.view == BOTH) }
            .values.map { it.region }.toSet()

    private const val BOTH = "both"
}
