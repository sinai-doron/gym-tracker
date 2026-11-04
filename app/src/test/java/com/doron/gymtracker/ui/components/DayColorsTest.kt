package com.doron.gymtracker.ui.components

import androidx.compose.ui.graphics.Color
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType
import org.junit.Assert.assertEquals
import org.junit.Test

class DayColorsTest {

    private fun type(id: String, color: String, order: Int) =
        TrainingType(id, null, id, color, Recorder.DURATION, null, null, true, order, false)

    private val strength = type("strength", "#6C5CE7", 1)
    private val run = type("run", "#00B894", 2)
    private val walk = type("walk", "#00B894", 3)
    private val pilates = type("pilates", "#E17055", 10)

    @Test
    fun `a type's colour is its hex`() {
        assertEquals(Color(0xFFE17055), pilates.colorValue())
        assertEquals(Color.Gray, pilates.copy(color = "pink").colorValue())
    }

    @Test
    fun `one colour per colour, strength last so it sits on the right`() {
        assertEquals(listOf(Color(0xFFE17055), Color(0xFF00B894), Color(0xFF6C5CE7)), dayColors(listOf(strength, run, walk, pilates)))
        assertEquals(listOf(Color(0xFF00B894)), dayColors(listOf(run, walk)))
        assertEquals(emptyList<Color>(), dayColors(emptyList()))
    }
}
