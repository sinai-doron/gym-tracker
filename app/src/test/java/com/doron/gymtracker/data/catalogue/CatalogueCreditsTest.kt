package com.doron.gymtracker.data.catalogue

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogueCreditsTest {

    private val credits = CatalogueCredits.parse(File("src/main/assets/${ExerciseCatalogue.ASSET}").readText())

    @Test
    fun `every exercise taken from wger names its page, authors and licence`() {
        assertTrue(credits.license.startsWith("CC BY-SA 4.0"))
        assertTrue(credits.credited.isNotEmpty())
        credits.credited.forEach { e ->
            val source = e.source!!
            val name = e.names.getValue("en")
            assertEquals(name, "wger", source.name)
            assertTrue(name, source.url.startsWith("https://wger.de/exercise/"))
            // Instructions copied from wger always name who wrote them; a few wger exercises name no one
            if (source.textLicense != null) assertTrue(name, source.textAuthors.isNotEmpty())
            assertTrue(name, source.licenses.isNotEmpty())
        }
    }

    @Test
    fun `authors and licences are listed once each`() {
        val source = ExerciseSource(
            name = "wger", url = "https://wger.de/exercise/1/view/",
            exerciseLicense = "CC-BY-SA 4", exerciseAuthors = listOf("a", "b"),
            textLicense = "CC-BY-SA 4", textAuthors = listOf("b", "c")
        )
        assertEquals(listOf("a", "b", "c"), source.authors)
        assertEquals(listOf("CC-BY-SA 4"), source.licenses)
    }
}
