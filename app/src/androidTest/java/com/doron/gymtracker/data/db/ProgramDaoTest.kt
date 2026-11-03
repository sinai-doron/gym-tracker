package com.doron.gymtracker.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.db.entities.ProgramEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The programme builder's queries, on real SQLite. */
@RunWith(AndroidJUnit4::class)
class ProgramDaoTest {

    private lateinit var db: GymDatabase

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDatabase::class.java)
            .allowMainThreadQueries().build()
        db.programDao().insert(ProgramEntity("abc", null, 20752, 6, "ALTERNATE_WEEKLY", isActive = true))
        db.programDao().insert(ProgramEntity("ul", "Upper / Lower", 20760, null, "NONE", isActive = false))
        db.workoutDao().insertAll(
            listOf(
                WorkoutEntity("a", "A", programId = "abc", sortOrder = 1),
                WorkoutEntity("b", "B", programId = "abc", sortOrder = 2),
                WorkoutEntity("ua", "UA", programId = "ul", sortOrder = 1),
            )
        )
    }

    @After
    fun teardown() = db.close()

    @Test
    fun makingOneActiveLeavesOnlyIt() = runTest {
        db.programDao().setArchived("ul", 5)
        db.programDao().makeActive("ul")
        assertEquals("ul", db.programDao().getActive()?.id)
        assertEquals(listOf("ul"), db.programDao().getAll().filter { it.isActive }.map { it.id })
        assertNull("making an archived programme active brings it back", db.programDao().get("ul")?.archivedAt)
    }

    @Test
    fun theActiveProgrammeCannotBeArchived() = runTest {
        db.programDao().setArchived("abc", 5)
        assertNull(db.programDao().get("abc")?.archivedAt)
        db.programDao().setArchived("ul", 5)
        assertEquals(5L, db.programDao().get("ul")?.archivedAt)
    }

    @Test
    fun sessionsAreCountedThroughTheProgrammesWorkouts() = runTest {
        db.sessionDao().insert(SessionEntity(0, "a", 1, 2))
        db.sessionDao().insert(SessionEntity(0, "b", 3, 4))
        db.sessionDao().insert(SessionEntity(0, "RUNNING", 5, 6, type = "RUNNING"))
        assertEquals(2, db.programDao().sessionCount("abc"))
        assertEquals(0, db.programDao().sessionCount("ul"))
        assertEquals(1, db.workoutDao().sessionCount("a"))
    }

    @Test
    fun aHiddenWorkoutLeavesTheRotationButStaysFindable() = runTest {
        db.workoutDao().hideWorkout("a", 9)
        assertEquals(listOf("b"), db.workoutDao().getForProgram("abc").map { it.id })
        assertEquals(1, db.programDao().workoutCount("abc"))
        assertEquals(9L, db.workoutDao().getWorkout("a")?.hiddenAt)
    }
}
