package com.doron.gymtracker.data.catalogue

import androidx.compose.ui.graphics.vector.PathParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BodyMapTest {

    private val map = BodyMap.parse(File("src/main/assets/${BodyMap.ASSET}").readText())

    @Test
    fun `every muscle has its region on the views it names`() {
        LibraryVocabulary.MUSCLE_REGIONS.forEach { (muscle, r) ->
            val views = if (r.view == "both") listOf(BodyMap.FRONT, BodyMap.BACK) else listOf(r.view)
            views.forEach { view ->
                assertTrue("$muscle: no ${r.region} on the $view", map.views.getValue(view).parts.any { it.slug == r.region })
            }
        }
    }

    @Test
    fun `every path parses`() {
        map.views.values.forEach { view ->
            (listOf(view.outline) + view.parts.flatMap { it.paths }).forEach { d ->
                assertTrue(d.take(40), PathParser().parsePathString(d).toNodes().isNotEmpty())
            }
        }
    }

    @Test
    fun `the licence beside the map is the source's`() {
        assertEquals("MIT", map.source.license)
        assertTrue(File("src/main/assets/${BodyMap.LICENSE_ASSET}").readText().startsWith("MIT License"))
    }

    @Test
    fun `a region stands for its muscles on that view`() {
        assertEquals(listOf("front_delts", "side_delts"), BodyRegions.muscles("deltoids", BodyMap.FRONT))
        assertEquals(listOf("rear_delts"), BodyRegions.muscles("deltoids", BodyMap.BACK))
        assertEquals(listOf("lats", "upper_back"), BodyRegions.muscles("upper-back", BodyMap.BACK))
        assertEquals(listOf("triceps"), BodyRegions.muscles("triceps", BodyMap.FRONT))
        assertEquals(emptyList<String>(), BodyRegions.muscles("head", BodyMap.FRONT))
    }

    @Test
    fun `a tap adds the region's muscles, and takes them away once all are chosen`() {
        val front = listOf("front_delts", "side_delts")
        assertEquals(setOf("chest", "front_delts", "side_delts"), BodyRegions.toggle(setOf("chest"), front))
        assertEquals(setOf("front_delts", "side_delts"), BodyRegions.toggle(setOf("side_delts"), front))
        assertEquals(setOf("chest"), BodyRegions.toggle(setOf("chest", "front_delts", "side_delts"), front))
    }

    @Test
    fun `a region is drawn chosen on the views its chosen muscles show on`() {
        assertEquals(setOf("deltoids"), BodyRegions.highlighted(setOf("rear_delts"), BodyMap.BACK))
        assertEquals(emptySet<String>(), BodyRegions.highlighted(setOf("rear_delts"), BodyMap.FRONT))
        assertEquals(setOf("calves"), BodyRegions.highlighted(setOf("calves"), BodyMap.FRONT))
        assertEquals(setOf("calves"), BodyRegions.highlighted(setOf("calves"), BodyMap.BACK))
    }
}
