package com.doron.gymtracker.data.templates

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Against the real templates in assets/templates. */
class TemplateNameIndexTest {

    private val index = TemplateNameIndex(
        File("src/main/assets/${ProgramTemplate.DIR}").listFiles { f -> f.extension == "json" }!!
            .map { ProgramTemplate.parse(it.readText()) }
    )

    @Test
    fun `a programme set up in Hebrew reads in English, and the other way`() {
        assertEquals("Full body", index.program("גוף מלא", "en"))
        assertEquals("גוף מלא", index.program("Full body", "he"))
        assertEquals("Full body", index.program("Full body", "en"))
    }

    @Test
    fun `its workouts too`() {
        assertEquals("Full body B", index.workout("גוף מלא B", "en"))
        assertEquals("עליון A", index.workout("Upper A", "he"))
        assertEquals("Push", index.workout("דחיפה", "en"))
    }

    @Test
    fun `a name the user typed, or a language no template has, stays as saved`() {
        assertEquals("My heavy day", index.workout("My heavy day", "he"))
        assertEquals("My plan", index.program("My plan", "en"))
        assertEquals("גוף מלא", index.program("גוף מלא", "fr"))
    }
}
