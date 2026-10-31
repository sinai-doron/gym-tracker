package com.doron.gymtracker.sync

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.db.GymDatabase
import com.doron.gymtracker.data.db.SyncMigration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Upload and download against each other, on real SQLite: one phone's data turned into server
 * items by [SyncPayloads], applied by [SyncPull] to a fresh install's database, and compared. The
 * server stores items as it receives them, so this is the whole round trip but the network.
 */
@RunWith(AndroidJUnit4::class)
class SyncRoundTripTest {

    private lateinit var phoneA: GymDatabase
    private lateinit var phoneB: GymDatabase
    private val a get() = phoneA.openHelper.writableDatabase
    private val b get() = phoneB.openHelper.writableDatabase

    private fun freshDatabase(): GymDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(), GymDatabase::class.java,
    ).allowMainThreadQueries().addCallback(object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) = SyncMigration.installFresh(db)
    }).build()

    @Before
    fun setUp() {
        phoneA = freshDatabase()
        phoneB = freshDatabase()
        SyncPull(a).ensureTables()
        SyncPull(b).ensureTables()
    }

    @After
    fun tearDown() {
        phoneA.close()
        phoneB.close()
    }

    private fun SupportSQLiteDatabase.count(sql: String, vararg args: Any): Long =
        query(sql, args).use { it.moveToFirst(); it.getLong(0) }

    private fun SupportSQLiteDatabase.text(sql: String, vararg args: Any): String? =
        query(sql, args).use { if (it.moveToFirst() && !it.isNull(0)) it.getString(0) else null }

    /** A gym session with one set, a run with its GPS points, a pause and a song, and a route. */
    private fun fillPhoneA() {
        a.execSQL("INSERT INTO workouts (id, label) VALUES ('workout_a', 'Chest and back')")
        a.execSQL(
            "INSERT INTO exercise_definitions (id, workoutId, name, type, targetSets, restDurationSecs, targetReps, " +
                "targetDistanceM, isOptional, isPerSide, isBodyweight, sortOrder, isRemoved) " +
                "VALUES ('bench', 'workout_a', 'Bench press', 'REP', 3, 90, 8, NULL, 0, 0, 0, 0, 0)"
        )
        a.execSQL("INSERT INTO sessions (workoutId, startedAt, finishedAt, type) VALUES ('workout_a', 1000, 5000, 'GYM')")
        a.execSQL("INSERT INTO exercise_entries (sessionId, exerciseId) VALUES (1, 'bench')")
        a.execSQL(
            "INSERT INTO set_records (entryId, completedAt, type, reps, weightKg) VALUES (1, 2000, 'REP', 8, 62.5)"
        )
        a.execSQL(
            "INSERT INTO sessions (workoutId, startedAt, finishedAt, type, distanceMeters) VALUES ('RUNNING', 10000, 20000, 'RUNNING', 1234.5)"
        )
        a.execSQL(
            "INSERT INTO run_details (sessionId, targetType, lapMeters, steps, calories) VALUES (2, 'NONE', 1000, 1500, 80)"
        )
        a.execSQL("INSERT INTO run_pauses (sessionId, startedAt, endedAt) VALUES (2, 12000, 13000)")
        a.execSQL("INSERT INTO track_log (sessionId, startedAt, title, artist) VALUES (2, 11000, 'Song', 'Artist')")
        // 1,300 points: three chunks of up to 600.
        for (i in 0 until 1300) {
            a.execSQL(
                "INSERT INTO location_log (sessionId, timestamp, latitude, longitude, speed) VALUES (2, ?, ?, 34.78, 3.1)",
                arrayOf<Any?>(10000L + i, 32.08 + i * 1e-5),
            )
        }
        a.execSQL("INSERT INTO routes (name, distanceMeters, createdAt) VALUES ('Park loop', 3000, 1)")
        for (i in 0 until 5) {
            a.execSQL("INSERT INTO route_points (routeId, seq, latitude, longitude, elevationM) VALUES (1, ?, 32.1, 34.8, NULL)", arrayOf<Any?>(i))
        }
    }

    /** What phone A would upload, as the server would hand it to phone B, newest-first order scrambled. */
    private fun serverItemsFromA(): List<IncomingChange> {
        val payloads = SyncPayloads(a)
        val log = a.query("SELECT entity, uuid, changed_at, deleted FROM sync_changes").use { c ->
            buildList { while (c.moveToNext()) add(listOf(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3))) }
        }
        return log.flatMap { (entity, uuid, at, deleted) ->
            payloads.changes(entity as String, uuid as String, at as Long, deleted == 1)
        }.map { IncomingChange(it.entity, it.id, it.updatedAt, it.deleted, it.data) }
            .reversed() // children before parents: the inbox has to sort it out
    }

    @Test
    fun aFreshDatabaseRecordsChangesFromTheStart() {
        a.execSQL("INSERT INTO workouts (id, label) VALUES ('workout_x', 'X')")
        val uuid = a.text("SELECT uuid FROM workouts WHERE id = 'workout_x'")
        assertNotNull("a new row gets a uuid", uuid)
        assertEquals(1L, a.count("SELECT count(*) FROM sync_changes WHERE entity = 'workout' AND uuid = ?", uuid!!))
    }

    @Test
    fun everythingArrivesOnAFreshInstallInAnyOrder() {
        fillPhoneA()
        val items = serverItemsFromA()
        // Pages of four, children first: every reference has to wait for its parent.
        val pages = items.chunked(4)
        pages.forEachIndexed { i, page -> SyncPull(b).applyPage(page, (i + 1).toLong()) }

        assertEquals(0L, b.count("SELECT count(*) FROM sync_inbox"))
        assertEquals("downloads are not logged back as local changes", 0L, b.count("SELECT count(*) FROM sync_changes"))
        assertEquals(pages.size.toLong(), SyncPull(b).cursor())
        for (table in listOf("workouts", "exercise_definitions", "sessions", "exercise_entries", "set_records",
            "run_details", "run_pauses", "track_log", "routes", "route_points", "location_log")) {
            assertEquals(table, a.count("SELECT count(*) FROM `$table`"), b.count("SELECT count(*) FROM `$table`"))
        }
        assertEquals("62.5", b.text("SELECT weightKg FROM set_records"))
        assertEquals("RUNNING", b.text("SELECT workoutId FROM sessions WHERE type = 'RUNNING'"))
        assertEquals("bench", b.text("SELECT id FROM exercise_definitions"))
        assertEquals("Park loop", b.text("SELECT name FROM routes"))
        // Every GPS point back, in order.
        assertEquals(
            a.text("SELECT group_concat(latitude) FROM (SELECT latitude FROM location_log ORDER BY timestamp)"),
            b.text("SELECT group_concat(latitude) FROM (SELECT latitude FROM location_log ORDER BY timestamp)"),
        )
        // The same uuids, so the next upload from B updates A's items instead of copying them.
        assertEquals(
            a.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM set_records ORDER BY uuid)"),
            b.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM set_records ORDER BY uuid)"),
        )
    }

    @Test
    fun aFreshInstallsOwnPlanTakesTheServersIdsInsteadOfDoublingUp() {
        fillPhoneA()
        // Phone B created the plan itself on first launch, under its own uuids, waiting to upload.
        b.execSQL("INSERT INTO workouts (id, label) VALUES ('workout_a', 'Chest and back')")
        val ownUuid = b.text("SELECT uuid FROM workouts")!!
        SyncPull(b).applyPage(serverItemsFromA(), 1)

        assertEquals(1L, b.count("SELECT count(*) FROM workouts"))
        assertEquals(a.text("SELECT uuid FROM workouts"), b.text("SELECT uuid FROM workouts"))
        assertEquals("its own uuid's upload is dropped", 0L, b.count("SELECT count(*) FROM sync_changes WHERE uuid = ?", ownUuid))
    }

    @Test
    fun aNewerLocalChangeSurvivesTheDownloadAndAnOlderOneGives() {
        fillPhoneA()
        SyncPull(b).applyPage(serverItemsFromA(), 1)
        val workout = b.text("SELECT uuid FROM workouts")!!
        b.execSQL("UPDATE workouts SET label = 'Mine, later'")
        val mine = b.count("SELECT changed_at FROM sync_changes WHERE uuid = ?", workout)

        val older = IncomingChange("workout", workout, mine - 1, false, serverItemsFromA().first { it.id == workout }.data)
        SyncPull(b).applyPage(listOf(older), 2)
        assertEquals("Mine, later", b.text("SELECT label FROM workouts"))

        val newer = older.copy(updatedAt = mine + 1)
        SyncPull(b).applyPage(listOf(newer), 3)
        assertEquals("Chest and back", b.text("SELECT label FROM workouts"))
        assertEquals(0L, b.count("SELECT count(*) FROM sync_changes WHERE uuid = ?", workout))
    }

    @Test
    fun deletionsArriveAndAreNotEchoed() {
        fillPhoneA()
        SyncPull(b).applyPage(serverItemsFromA(), 1)
        val setUuid = b.text("SELECT uuid FROM set_records")!!
        val trackChunk = SyncPayloads.trackChunkId(b.text("SELECT uuid FROM sessions WHERE type = 'RUNNING'")!!, 2)

        SyncPull(b).applyPage(
            listOf(
                IncomingChange("set", setUuid, Long.MAX_VALUE / 2, true, null),
                IncomingChange(SyncPayloads.TRACK, trackChunk, Long.MAX_VALUE / 2, true, null),
            ),
            2,
        )
        assertEquals(0L, b.count("SELECT count(*) FROM set_records"))
        assertEquals("the last chunk's 100 points are gone", 1200L, b.count("SELECT count(*) FROM location_log"))
        assertEquals(0L, b.count("SELECT count(*) FROM sync_changes"))
        assertTrue(b.count("SELECT count(*) FROM sync_control WHERE applying = 0") == 1L)
    }
}
