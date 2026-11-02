package com.doron.gymtracker.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.db.dao.ExerciseLibraryDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntity
import com.doron.gymtracker.data.db.entities.ExerciseNameEntity
import com.doron.gymtracker.data.repository.ExerciseLibrary
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The library seeded from the catalogue the app ships, then re-seeded the way an app update would. */
@RunWith(AndroidJUnit4::class)
class ExerciseLibraryTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: GymDatabase
    private lateinit var dao: ExerciseLibraryDao
    private val catalogue = ExerciseCatalogue.parse(
        context.assets.open(ExerciseCatalogue.ASSET).bufferedReader().use { it.readText() }
    )
    private val benchId = ExerciseCatalogue.builtinId("barbell-bench-press")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java).allowMainThreadQueries().build()
        dao = db.exerciseLibraryDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun aFreshInstallGetsEveryBuiltIn() = runTest {
        ExerciseLibrary.seed(dao, catalogue, now = 1)

        assertEquals(catalogue.exercises.size, dao.builtinCount())
        assertEquals(catalogue.version, dao.builtinVersion())
        val bench = dao.get(benchId)!!
        assertEquals("barbell-bench-press", bench.slug)
        assertEquals("total", bench.load)
        assertTrue(dao.muscles(benchId).any { it.muscle == "chest" && it.isPrimary })
    }

    @Test
    fun searchFindsNamesInEitherLanguage() = runTest {
        ExerciseLibrary.seed(dao, catalogue, now = 1)

        assertTrue(dao.search("לחיצת חזה עם מוט").any { it.id == benchId })
        assertTrue(dao.search("bench press").any { it.id == benchId })
        assertTrue(dao.search("no such exercise").isEmpty())
    }

    @Test
    fun theSameCatalogueAgainChangesNothing() = runTest {
        ExerciseLibrary.seed(dao, catalogue, now = 1)
        ExerciseLibrary.seed(dao, catalogue, now = 2)

        assertEquals(catalogue.exercises.size, dao.builtinCount())
        assertNull(dao.get(benchId)!!.archivedAt)
    }

    /** An update that drops an exercise archives it (history may point at it) and leaves the user's own alone. */
    @Test
    fun aNewCatalogueUpdatesBuiltInsAndNeverTouchesTheUsersOwn() = runTest {
        ExerciseLibrary.seed(dao, catalogue, now = 1)
        val mine = ExerciseEntity("mine", null, "CUSTOM", "reps", "none", null, "both", "other", null, null, null)
        dao.upsertExercises(listOf(mine))
        dao.insertNames(listOf(ExerciseNameEntity("mine", "en", "My move", isAlias = false)))

        val next = catalogue.copy(
            version = catalogue.version + 1,
            exercises = catalogue.exercises.filter { it.slug != "crunch" }
                .map { if (it.slug == "barbell-bench-press") it.copy(equipment = "smith_machine") else it }
        )
        ExerciseLibrary.seed(dao, next, now = 5)

        assertEquals(next.exercises.size, dao.builtinCount())
        assertEquals("smith_machine", dao.get(benchId)!!.equipment)
        assertEquals(5L, dao.get(ExerciseCatalogue.builtinId("crunch"))!!.archivedAt)
        assertEquals(mine, dao.get("mine"))
        assertEquals("My move", dao.names("mine").single().name)
    }

    @Test
    fun thePlanIsLinkedOnceAndALinkIsKept() = runTest {
        val workouts = db.workoutDao()
        fun plan(id: String, link: String? = null) =
            ExerciseDefinitionEntity(id, "workout_a", id, "REP", 3, 60, 10, null, libraryExerciseId = link)
        workouts.insertAllExercises(listOf(plan("d1_e1"), plan("d2_e8a", link = "chosen-by-user"), plan("custom_1")))

        ExerciseLibrary.seed(dao, catalogue, now = 1)

        assertEquals(benchId, workouts.getExercise("d1_e1")!!.libraryExerciseId)
        assertEquals("chosen-by-user", workouts.getExercise("d2_e8a")!!.libraryExerciseId)
        assertNull(workouts.getExercise("custom_1")!!.libraryExerciseId)
        assertNotNull(dao.get(workouts.getExercise("d1_e1")!!.libraryExerciseId!!))
    }
}
