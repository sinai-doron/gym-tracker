package com.doron.gymtracker.sync

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.db.GymDatabase
import com.doron.gymtracker.data.db.SyncMigration
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The change-log triggers driven the way the app drives them: through Room's DAOs, which write
 * with INSERT OR ABORT and UPDATE OR ABORT. The first version passed plain-SQL tests and crashed
 * on the owner's phone when a run started (2026-10-31), because an outer OR ABORT overrides a
 * trigger's own conflict clause.
 */
@RunWith(AndroidJUnit4::class)
class SyncTriggersThroughRoomTest {

    private lateinit var db: GymDatabase
    private val sql get() = db.openHelper.writableDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GymDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = SyncMigration.installFresh(db)
            })
            .build()
        sql.execSQL("INSERT INTO workouts (id, label) VALUES ('workout_a', 'A')")
        sql.execSQL(
            "INSERT INTO exercise_definitions (id, workoutId, name, type, targetSets, restDurationSecs, isOptional, " +
                "isPerSide, isBodyweight, sortOrder, isRemoved) VALUES ('bench', 'workout_a', 'Bench', 'REP', 3, 90, 0, 0, 0, 0, 0)"
        )
    }

    @After
    fun tearDown() = db.close()

    private fun pending(kind: String): Long =
        sql.query("SELECT count(*) FROM sync_changes WHERE entity = ?", arrayOf(kind)).use { it.moveToFirst(); it.getLong(0) }

    @Test
    fun startingARunAndRecordingItWorks() = runTest {
        val id = db.sessionDao().insert(SessionEntity(0, "RUNNING", 1000, null, "RUNNING"))
        repeat(5) { i -> db.locationLogDao().insert(LocationLogEntity(sessionId = id, timestamp = 1000L + i, latitude = 32.0, longitude = 34.0, speed = null, altitude = null)) }
        val session = db.sessionDao().getById(id)!!
        db.sessionDao().update(session.copy(finishedAt = 9000, distanceMeters = 12.0))
        // Built without its uuid, as code that makes a fresh entity would: the uuid survives.
        db.sessionDao().update(SessionEntity(id, "RUNNING", 1000, 9500, "RUNNING", 13.0))

        assertNotNull(db.sessionDao().getById(id)!!.uuid)
        assertEquals(1L, pending("session"))
        assertEquals(1L, pending("track"))
    }

    @Test
    fun editingTheSameSetTwiceBeforeASyncWorks() = runTest {
        val session = db.sessionDao().insert(SessionEntity(0, "workout_a", 1000, null, "GYM"))
        val entry = db.exerciseEntryDao().insert(ExerciseEntryEntity(0, session, "bench"))
        val setId = db.setRecordDao().insert(SetRecordEntity(0, entry, 2000, "REP", 8, 60f, null, null))
        val set = SetRecordEntity(setId, entry, 2000, "REP", 8, 60f, null, null)
        db.setRecordDao().update(set.copy(reps = 9))
        db.setRecordDao().update(set.copy(reps = 10))
        db.setRecordDao().deleteById(setId)

        assertEquals(1L, pending("set"))
        assertEquals(
            "the last change is the deletion", 1L,
            sql.query("SELECT deleted FROM sync_changes WHERE entity = 'set'").use { it.moveToFirst(); it.getLong(0) },
        )
    }
}
