package com.doron.gymtracker.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.repository.ExerciseLibrary
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** What the exercise picker reads and writes, on a database made the way the app makes one. */
@RunWith(AndroidJUnit4::class)
class ExercisePickerDataTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: GymDatabase
    private lateinit var library: ExerciseLibrary
    private val sql get() = db.openHelper.writableDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, GymDatabase::class.java).allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = GymDatabase.installFresh(db)
            }).build()
        library = ExerciseLibrary(context, db.exerciseLibraryDao())
    }

    @After
    fun tearDown() = db.close()

    private fun count(q: String) = sql.query(q).use { it.moveToFirst(); it.getLong(0) }

    @Test
    fun everyBuiltInIsNamedInTheAppsLanguageWithItsDefaults() = runTest {
        val bench = library.all("he").first { it.id == ExerciseCatalogue.builtinId("barbell-bench-press") }
        assertEquals("לחיצת חזה עם מוט (שטוח)", bench.name)
        assertTrue("its English name and aliases are searchable too", "bench press" in bench.searchNames)
        assertEquals(listOf(3, 6, 10, 120), listOf(bench.defaults.sets, bench.defaults.repsMin, bench.defaults.repsMax, bench.defaults.restS))
        assertEquals("Barbell bench press", library.all("en").first { it.id == bench.id }.name)
        assertEquals(192, library.all("en").size)
    }

    @Test
    fun anExerciseOfTheUsersOwnIsListedAndQueuedForSyncButBuiltInsAreNot() = runTest {
        library.all("en") // seeds the 192 built-ins
        assertEquals("built-ins never queue", 0L, count("SELECT count(*) FROM sync_changes WHERE entity = 'library_exercise'"))

        val id = library.createCustom("Landmine press", "en", "reps_load", "barbell", "front_delts")

        val mine = library.all("he").first { it.id == id }
        assertEquals("Landmine press", mine.name) // no Hebrew name: the one it has
        assertTrue(mine.isCustom)
        assertEquals(listOf("front_delts"), mine.primaryMuscles)
        assertEquals(1L, count("SELECT count(*) FROM sync_changes WHERE entity = 'library_exercise' AND uuid = '$id'"))
        assertEquals("total|2.5", sql.query("SELECT load || '|' || weightStepKg FROM exercises WHERE id = '$id'").use { it.moveToFirst(); it.getString(0) })
    }

    @Test
    fun recentlyDoneIsByTheLastTimeEachWasDone() = runTest {
        val squat = ExerciseCatalogue.builtinId("barbell-back-squat")
        val bench = ExerciseCatalogue.builtinId("barbell-bench-press")
        db.workoutDao().insertAll(listOf(WorkoutEntity("w", "A", programId = "p")))
        db.workoutDao().insertAllExercises(
            listOf(
                ExerciseDefinitionEntity("s", "w", "Squat", "REP", 3, 90, 5, null, libraryExerciseId = squat),
                ExerciseDefinitionEntity("b", "w", "Bench", "REP", 3, 90, 5, null, libraryExerciseId = bench),
            )
        )
        val older = db.sessionDao().insert(SessionEntity(0, "w", 1_000, 2_000))
        val newer = db.sessionDao().insert(SessionEntity(0, "w", 5_000, 6_000))
        db.exerciseEntryDao().insert(ExerciseEntryEntity(0, older, "s"))
        db.exerciseEntryDao().insert(ExerciseEntryEntity(0, older, "b"))
        db.exerciseEntryDao().insert(ExerciseEntryEntity(0, newer, "b"))
        assertEquals(listOf(bench, squat), library.recentlyDone())
    }
}
