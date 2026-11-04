package com.doron.gymtracker.data.catalogue

import com.doron.gymtracker.data.db.LibraryMigration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The catalogue the app ships, read the way the app reads it. */
class ExerciseCatalogueTest {

    // Unit tests run in the module directory
    private val catalogue = ExerciseCatalogue.parse(File("src/main/assets/${ExerciseCatalogue.ASSET}").readText())

    @Test
    fun `every exercise in the shipped catalogue reads`() {
        assertTrue("expected the full catalogue, got ${catalogue.exercises.size}", catalogue.exercises.size >= 190)
        assertEquals(catalogue.exercises.size, catalogue.exercises.map { it.slug }.toSet().size)
        catalogue.exercises.forEach { e ->
            assertTrue("${e.slug} has no English name", !e.names["en"].isNullOrBlank())
            assertTrue("${e.slug} has no Hebrew name", !e.names["he"].isNullOrBlank())
            assertTrue("${e.slug} has no primary muscle", e.muscles.primary.isNotEmpty())
        }
    }

    /** Synced history and the v14 migration hold ids made from slugs: the derivation must never change. */
    @Test
    fun `a built-in's id is derived from its slug and never changes`() {
        assertEquals("936c29b1-a11d-3212-9375-b4430e5930b2", ExerciseCatalogue.builtinId("barbell-bench-press"))
        assertEquals(ExerciseCatalogue.builtinId("crunch"), ExerciseCatalogue.builtinId("crunch"))
        assertTrue(ExerciseCatalogue.builtinId("crunch") != ExerciseCatalogue.builtinId("plank"))
    }

    /** The migration's frozen map and the catalogue's legacy ids must name the same built-ins. */
    @Test
    fun `the migration links the plan to the built-ins the catalogue names`() {
        val frozen = LibraryMigration.PLAN_TO_BUILTIN.mapValues { (_, slug) -> ExerciseCatalogue.builtinId(slug) }
        assertEquals(frozen, catalogue.legacyIds)
    }

    @Test
    fun `entities carry names, aliases and muscles`() {
        val (exercises, names, muscles) = catalogue.toEntities()
        assertEquals(catalogue.exercises.size, exercises.size)
        val bench = exercises.single { it.slug == "barbell-bench-press" }
        assertEquals("reps_load", bench.preset)
        assertEquals(catalogue.version, bench.catalogueVersion)
        val benchNames = names.filter { it.exerciseId == bench.id }
        assertTrue(benchNames.any { it.lang == "he" && it.name == "לחיצת חזה עם מוט (שטוח)" && !it.isAlias })
        assertTrue(benchNames.any { it.lang == "en" && !it.isAlias })
        assertTrue(muscles.any { it.exerciseId == bench.id && it.muscle == "chest" && it.isPrimary })
    }

    @Test
    fun `each exercise carries its default targets`() {
        val bench = catalogue.exercises.first { it.slug == "barbell-bench-press" }.defaults
        assertEquals(listOf(3, 6, 10, 120), listOf(bench.sets, bench.repsMin, bench.repsMax, bench.restS))
        val plank = catalogue.exercises.first { it.slug == "plank" }.defaults
        assertEquals(45, plank.durationS)
    }
}
