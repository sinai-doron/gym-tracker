package com.doron.gymtracker.data.places

import com.doron.gymtracker.data.db.dao.TrackExtent
import com.doron.gymtracker.domain.usecase.GetPlaceBadgesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlaceAtlasTest {

    private val places = PlaceAtlas.parse(File("src/main/assets/${PlaceAtlas.ASSET}").readText())

    private fun at(lat: Double, lon: Double) = PlaceAtlas.locate(places, lat, lon).map { it.id }

    @Test
    fun `the atlas has the three countries and the fifty states, named in both languages`() {
        assertEquals(setOf("usa", "israel", "uk"), places.filter { it.kind == Place.Kind.COUNTRY }.map { it.id }.toSet())
        assertEquals(50, places.count { it.kind == Place.Kind.STATE })
        assertTrue(places.all { it.names["en"]?.isNotBlank() == true && it.names["he"]?.isNotBlank() == true })
    }

    @Test
    fun `the owner's runs abroad and at home are placed`() {
        assertEquals(listOf("uk"), at(51.5095, -0.1372))                 // London
        assertEquals(listOf("usa", "us-nv"), at(36.0711, -115.1718))     // Las Vegas
        assertEquals(listOf("usa", "us-ny"), at(40.7751, -73.9725))      // Central Park
        assertEquals(listOf("usa", "us-ca"), at(37.7937, -122.2736))     // Oakland
        assertEquals(listOf("israel"), at(32.565, 34.958))               // Zichron Ya'akov
    }

    @Test
    fun `washington is in the USA but in no state`() {
        assertEquals(listOf("usa"), at(38.8895, -77.0353))
    }

    @Test
    fun `outside the three countries there is no badge`() {
        assertEquals(emptyList<String>(), at(30.084, 31.352))            // Cairo airport
        assertEquals(emptyList<String>(), at(48.8566, 2.3522))           // Paris
    }

    @Test
    fun `a run whose GPS sat on one spot hasn't moved`() {
        // A jammed run on 17 February 2024: every point at Cairo's airport
        assertFalse(GetPlaceBadgesUseCase.moved(TrackExtent(1, 40, 30.0840, 30.0841, 31.3520, 31.3521)))
        // A 2 km loop in Las Vegas
        assertTrue(GetPlaceBadgesUseCase.moved(TrackExtent(2, 900, 36.065, 36.078, -115.175, -115.168)))
    }
}
