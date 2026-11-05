package com.doron.gymtracker.data.catalogue

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** LibraryVocabulary and the label strings were copied from catalogue/vocabulary.json; they must still match it. */
class LibraryVocabularyTest {

    private val vocabulary = Json.parseToJsonElement(File("../catalogue/vocabulary.json").readText()).jsonObject

    private fun labels(dir: String): Map<String, String> =
        Regex("""<string name="([a-z_]+)">([^<]*)</string>""")
            .findAll(File("src/main/res/$dir/strings_library.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\'", "'") }

    @Test
    fun `muscles, equipment and presets are the vocabulary's, in its order`() {
        assertEquals(vocabulary.getValue("muscles").jsonObject.keys.toList(), LibraryVocabulary.MUSCLES)
        assertEquals(vocabulary.getValue("equipment").jsonObject.keys.toList(), LibraryVocabulary.EQUIPMENT_STEP_KG.keys.toList())
        assertEquals(vocabulary.getValue("presets").jsonObject.keys.toList(), LibraryVocabulary.PRESETS)
    }

    @Test
    fun `muscle regions are the vocabulary's`() {
        vocabulary.getValue("muscles").jsonObject.forEach { (slug, value) ->
            val muscle = value.jsonObject
            assertEquals(
                slug,
                MuscleRegion(muscle.getValue("region").jsonPrimitive.content, muscle.getValue("view").jsonPrimitive.content),
                LibraryVocabulary.MUSCLE_REGIONS[slug]
            )
        }
        assertEquals(LibraryVocabulary.MUSCLES, LibraryVocabulary.MUSCLE_REGIONS.keys.toList())
    }

    @Test
    fun `weight steps are the vocabulary's`() {
        vocabulary.getValue("equipment").jsonObject.forEach { (slug, value) ->
            val step = value.jsonObject.getValue("weightStepKg").let { if (it is JsonNull) null else it.jsonPrimitive.doubleOrNull }
            assertEquals(slug, step, LibraryVocabulary.EQUIPMENT_STEP_KG[slug])
        }
    }

    @Test
    fun `every word has its English and Hebrew label`() {
        // English is the default; Hebrew is the translation
        val en = labels("values")
        val he = labels("values-iw")
        for ((group, prefix) in listOf("muscles" to "muscle_", "equipment" to "equipment_", "presets" to "preset_")) {
            vocabulary.getValue(group).jsonObject.forEach { (slug, value) ->
                assertEquals(value.jsonObject.getValue("en").jsonPrimitive.content, en["$prefix$slug"])
                assertEquals(value.jsonObject.getValue("he").jsonPrimitive.content, he["$prefix$slug"])
            }
        }
    }
}
