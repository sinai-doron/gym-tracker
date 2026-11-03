package com.doron.gymtracker.ui.screens.setup

import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.data.prefs.Profile
import com.doron.gymtracker.data.prefs.WeekStart
import com.doron.gymtracker.data.prefs.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class SetupTest {

    @Test
    fun `units and week start follow the region`() {
        assertEquals(Profile(WeightUnit.LB, DistanceUnit.MI, WeekStart.SUNDAY), Profile.defaultsFor(Locale.US))
        assertEquals(Profile(WeightUnit.KG, DistanceUnit.MI, WeekStart.MONDAY), Profile.defaultsFor(Locale.UK))
        assertEquals(Profile(WeightUnit.KG, DistanceUnit.KM, WeekStart.SUNDAY), Profile.defaultsFor(Locale.forLanguageTag("he-IL")))
        assertEquals(Profile(WeightUnit.KG, DistanceUnit.KM, WeekStart.MONDAY), Profile.defaultsFor(Locale.GERMANY))
    }

    @Test
    fun `body weight is kept in kg, whatever it was typed in`() {
        assertEquals(78f, AboutYouViewModel.parseWeightKg("78", WeightUnit.KG)!!, 0.001f)
        assertEquals(78.5f, AboutYouViewModel.parseWeightKg("78,5", WeightUnit.KG)!!, 0.001f)
        assertEquals(77.11f, AboutYouViewModel.parseWeightKg("170", WeightUnit.LB)!!, 0.01f)
        assertNull(AboutYouViewModel.parseWeightKg("", WeightUnit.KG))
        assertEquals("78", AboutYouViewModel.formatWeight(78f, WeightUnit.KG))
        assertEquals("172", AboutYouViewModel.formatWeight(78f, WeightUnit.LB))
        assertEquals("78.5", AboutYouViewModel.formatWeight(78.5f, WeightUnit.KG))
    }

    @Test
    fun `week 1 starts on the first day of this week`() {
        val tuesday = LocalDate.of(2026, 11, 3)
        assertEquals(LocalDate.of(2026, 11, 1), TrainingViewModel.weekStartOf(tuesday, WeekStart.SUNDAY))
        assertEquals(LocalDate.of(2026, 11, 2), TrainingViewModel.weekStartOf(tuesday, WeekStart.MONDAY))
        assertEquals(LocalDate.of(2026, 11, 1), TrainingViewModel.weekStartOf(LocalDate.of(2026, 11, 1), WeekStart.SUNDAY))
    }
}
