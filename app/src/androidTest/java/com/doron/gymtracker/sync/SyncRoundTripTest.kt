package com.doron.gymtracker.sync

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.db.GymDatabase
import com.doron.gymtracker.data.db.SyncMigration
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        override fun onCreate(db: SupportSQLiteDatabase) = GymDatabase.installFresh(db)
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
        a.execSQL(
            "INSERT INTO programs (id, name, startEpochDay, deloadEveryWeeks, variantRule, isActive) " +
                "VALUES ('program_abc', NULL, 20752, 6, 'ALTERNATE_WEEKLY', 1)"
        )
        a.execSQL("INSERT INTO workouts (id, label, programId, sortOrder) VALUES ('workout_a', 'Chest and back', 'program_abc', 1)")
        a.execSQL(
            "INSERT INTO exercise_definitions (id, workoutId, name, type, targetSets, restDurationSecs, targetReps, " +
                "targetDistanceM, isOptional, isPerSide, isBodyweight, sortOrder, isRemoved) " +
                "VALUES ('bench', 'workout_a', 'Bench press', 'REP', 3, 90, 8, NULL, 0, 0, 0, 0, 0)"
        )
        a.execSQL("INSERT INTO sessions (workoutId, startedAt, finishedAt, type, activityTypeId) VALUES ('workout_a', 1000, 5000, 'GYM', 'strength')")
        a.execSQL("INSERT INTO exercise_entries (sessionId, exerciseId) VALUES (1, 'bench')")
        a.execSQL("INSERT INTO sets (entryId, seq, kind, completedAt) VALUES (1, 1, 'WORKING', 2000)")
        a.execSQL("INSERT INTO set_parts (setId, seq, reps, loadKg) VALUES (1, 1, 8, 62.5)")
        a.execSQL(
            "INSERT INTO sessions (workoutId, startedAt, finishedAt, type, distanceMeters, activityTypeId) " +
                "VALUES ('RUNNING', 10000, 20000, 'RUNNING', 1234.5, 'run')"
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
        a.execSQL("INSERT INTO workouts (id, label, programId, sortOrder) VALUES ('workout_x', 'X', 'program_abc', 1)")
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
        for (table in listOf("activity_types", "programs", "workouts", "exercise_definitions", "sessions", "exercise_entries", "sets", "set_parts",
            "run_details", "run_pauses", "track_log", "routes", "route_points", "location_log")) {
            assertEquals(table, a.count("SELECT count(*) FROM `$table`"), b.count("SELECT count(*) FROM `$table`"))
        }
        assertEquals("62.5", b.text("SELECT loadKg FROM set_parts"))
        assertEquals("RUNNING", b.text("SELECT workoutId FROM sessions WHERE type = 'RUNNING'"))
        assertEquals("bench", b.text("SELECT id FROM exercise_definitions"))
        assertEquals("program_abc|20752|6|ALTERNATE_WEEKLY|1", b.text(
            "SELECT id || '|' || startEpochDay || '|' || deloadEveryWeeks || '|' || variantRule || '|' || isActive FROM programs"
        ))
        assertEquals("program_abc|1", b.text("SELECT programId || '|' || sortOrder FROM workouts"))
        assertEquals("Park loop", b.text("SELECT name FROM routes"))
        // Every GPS point back, in order.
        assertEquals(
            a.text("SELECT group_concat(latitude) FROM (SELECT latitude FROM location_log ORDER BY timestamp)"),
            b.text("SELECT group_concat(latitude) FROM (SELECT latitude FROM location_log ORDER BY timestamp)"),
        )
        // The same uuids, so the next upload from B updates A's items instead of copying them.
        assertEquals(
            a.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM sets ORDER BY uuid)"),
            b.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM sets ORDER BY uuid)"),
        )
    }

    @Test
    fun aFreshInstallsOwnPlanTakesTheServersIdsInsteadOfDoublingUp() {
        fillPhoneA()
        // Phone B created the plan itself on first launch, under its own uuids, waiting to upload.
        b.execSQL("INSERT INTO workouts (id, label, programId, sortOrder) VALUES ('workout_a', 'Chest and back', 'program_abc', 1)")
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
    fun aRunsStepsStayOnThePhoneThroughSyncAndGoWithTheRun() {
        fillPhoneA()
        for (i in 0 until 10) a.execSQL("INSERT INTO run_steps (sessionId, timestamp, steps) VALUES (2, ?, ?)", arrayOf<Any?>(10000L + i * 5000, i * 14))
        val items = serverItemsFromA()
        assertTrue(items.none { it.data.toString().contains("steps\":[") })

        // The phone that recorded the run pulls its own upload back, track and all
        SyncPull(a).applyPage(items, 1)
        assertEquals(10L, a.count("SELECT count(*) FROM run_steps"))
        assertEquals(1300L, a.count("SELECT count(*) FROM location_log"))

        val run = a.text("SELECT uuid FROM sessions WHERE type = 'RUNNING'")!!
        SyncPull(a).applyPage(listOf(IncomingChange("session", run, Long.MAX_VALUE / 2, true, null)), 2)
        assertEquals(0L, a.count("SELECT count(*) FROM run_steps"))
    }

    @Test
    fun deletionsArriveAndAreNotEchoed() {
        fillPhoneA()
        SyncPull(b).applyPage(serverItemsFromA(), 1)
        val setUuid = b.text("SELECT uuid FROM sets")!!
        val trackChunk = SyncPayloads.trackChunkId(b.text("SELECT uuid FROM sessions WHERE type = 'RUNNING'")!!, 2)

        SyncPull(b).applyPage(
            listOf(
                IncomingChange("set", setUuid, Long.MAX_VALUE / 2, true, null),
                IncomingChange(SyncPayloads.TRACK, trackChunk, Long.MAX_VALUE / 2, true, null),
            ),
            2,
        )
        assertEquals(0L, b.count("SELECT count(*) FROM sets"))
        assertEquals(0L, b.count("SELECT count(*) FROM set_parts"))
        assertEquals("the last chunk's 100 points are gone", 1200L, b.count("SELECT count(*) FROM location_log"))
        assertEquals(0L, b.count("SELECT count(*) FROM sync_changes"))
        assertTrue(b.count("SELECT count(*) FROM sync_control WHERE applying = 0") == 1L)
    }

    /** A set uploaded by an app from before version 15 arrives as one row; it becomes a set with one part. */
    @Test
    fun aSetInTheOldShapeBecomesASetWithOnePart() {
        fillPhoneA()
        val items = serverItemsFromA()
        val set = items.single { it.entity == "set" }
        val entry = set.data!!["entryId"]!!
        fun old(reps: Int, weightKg: Double) = buildJsonObject {
            put("entryId", entry)
            put("completedAt", 2000)
            put("type", "REP")
            put("reps", reps)
            put("weightKg", weightKg)
            put("durationSecs", JsonNull)
            put("distanceM", JsonNull)
        }

        SyncPull(b).applyPage(items.map { if (it.entity == "set") it.copy(data = old(8, 62.5)) else it }, 1)
        assertEquals("1|WORKING|8|62.5", b.text("SELECT s.seq || '|' || s.kind || '|' || p.reps || '|' || p.loadKg FROM sets s JOIN set_parts p ON p.setId = s.id"))

        // Saved as 0 kg before version 15: no load
        SyncPull(b).applyPage(listOf(set.copy(updatedAt = set.updatedAt + 1, data = old(12, 0.0))), 2)
        assertEquals(1L, b.count("SELECT count(*) FROM set_parts"))
        assertEquals("12|", b.text("SELECT p.reps || '|' || coalesce(p.loadKg, '') FROM set_parts p"))
    }

    /** A workout uploaded before version 16 names no programme: it joins the seeded one, in its letter's place. */
    @Test
    fun aWorkoutInTheOldShapeJoinsTheSeededProgramme() {
        val old = IncomingChange(
            "workout", java.util.UUID.randomUUID().toString(), 1_000, false,
            buildJsonObject { put("key", "workout_b"); put("label", "B") },
        )
        SyncPull(b).applyPage(listOf(old), 1)
        assertEquals("program_abc|2", b.text("SELECT programId || '|' || sortOrder FROM workouts WHERE id = 'workout_b'"))
    }

    @Test
    fun aTrainingTypeOfTheUsersOwnAndASessionLoggedByHandArrive() {
        a.execSQL(
            "INSERT INTO activity_types (id, name, icon, color, recorder, healthConnectType, metValue, builtin, sortOrder) " +
                "VALUES ('f0072b11-0000-4000-8000-000000000001', 'Football', 'football', '#E17055', 'DURATION', 64, 7.0, 0, 4)"
        )
        a.execSQL(
            "INSERT INTO sessions (workoutId, startedAt, finishedAt, type, activityTypeId, loggedManually, durationS, calories, note) " +
                "VALUES ('OTHER', 1000, 5401000, 'OTHER', 'f0072b11-0000-4000-8000-000000000001', 1, 5400, 780, '5-a-side, won 7-5')"
        )
        // A built-in hidden by the user travels too
        a.execSQL("UPDATE activity_types SET hiddenAt = 99 WHERE id = 'walk'")

        serverItemsFromA().chunked(2).forEachIndexed { i, page -> SyncPull(b).applyPage(page, (i + 1).toLong()) }

        assertEquals(0L, b.count("SELECT count(*) FROM sync_inbox"))
        assertEquals("Football|DURATION|64", b.text("SELECT name || '|' || recorder || '|' || healthConnectType FROM activity_types WHERE builtin = 0"))
        assertEquals(
            "OTHER|f0072b11-0000-4000-8000-000000000001|1|5400|780|5-a-side, won 7-5",
            b.text("SELECT type || '|' || activityTypeId || '|' || loggedManually || '|' || durationS || '|' || calories || '|' || note FROM sessions"),
        )
        assertEquals("99", b.text("SELECT hiddenAt FROM activity_types WHERE id = 'walk'"))
        // B's own built-ins took A's uuids instead of doubling up
        assertEquals(3L, b.count("SELECT count(*) FROM activity_types WHERE builtin = 1"))
        assertEquals(
            a.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM activity_types ORDER BY id)"),
            b.text("SELECT group_concat(uuid) FROM (SELECT uuid FROM activity_types ORDER BY id)"),
        )
    }

    @Test
    fun anExerciseOfTheUsersOwnArrivesWithItsNamesAndMusclesAndTheBuiltInsStayHome() {
        val custom = "c0ffee00-0000-4000-8000-000000000002"
        a.execSQL(
            "INSERT INTO exercises (id, slug, source, preset, load, sides, equipment, mechanics, weightStepKg, catalogueVersion) " +
                "VALUES ('$custom', NULL, 'CUSTOM', 'reps_load', 'total', 'both', 'barbell', 'compound', 2.5, NULL)"
        )
        a.execSQL("INSERT INTO exercise_names VALUES ('$custom', 'en', 'Landmine press', 0)")
        a.execSQL("INSERT INTO exercise_muscles VALUES ('$custom', 'front_delts', 1), ('$custom', 'triceps', 0)")
        // A built-in written the way the catalogue writes one: never queued
        a.execSQL(
            "INSERT INTO exercises (id, slug, source, preset, load, sides, equipment, mechanics, weightStepKg, catalogueVersion) " +
                "VALUES ('936c29b1-a11d-3212-9375-b4430e5930b2', 'barbell-bench-press', 'BUILTIN', 'reps_load', 'total', 'both', 'barbell', 'compound', 2.5, 1)"
        )
        a.execSQL("INSERT INTO exercise_names VALUES ('936c29b1-a11d-3212-9375-b4430e5930b2', 'en', 'Barbell bench press', 0)")
        assertEquals(1L, a.count("SELECT count(*) FROM sync_changes WHERE entity = 'library_exercise'"))

        a.execSQL(
            "INSERT INTO programs (id, name, startEpochDay, deloadEveryWeeks, variantRule, isActive) VALUES ('p', 'Mine', 20760, NULL, 'NONE', 1)"
        )
        a.execSQL("INSERT INTO workouts (id, label, programId, sortOrder) VALUES ('w', 'UA', 'p', 1)")
        a.execSQL(
            "INSERT INTO exercise_definitions (id, workoutId, name, type, targetSets, restDurationSecs, targetReps, targetRepsMin, " +
                "targetDistanceM, isOptional, isPerSide, isBodyweight, sortOrder, isRemoved, libraryExerciseId) " +
                "VALUES ('x', 'w', 'Landmine press', 'REP', 3, 90, 10, 6, NULL, 0, 0, 0, 1, 0, '$custom')"
        )

        SyncPull(b).applyPage(serverItemsFromA(), 1)

        assertEquals(0L, b.count("SELECT count(*) FROM sync_inbox"))
        assertEquals("CUSTOM|barbell|2.5", b.text("SELECT source || '|' || equipment || '|' || weightStepKg FROM exercises WHERE id = '$custom'"))
        assertEquals("Landmine press", b.text("SELECT name FROM exercise_names WHERE exerciseId = '$custom'"))
        assertEquals("front_delts:1,triceps:0", b.text(
            "SELECT group_concat(m) FROM (SELECT muscle || ':' || isPrimary AS m FROM exercise_muscles WHERE exerciseId = '$custom' ORDER BY muscle)"
        ))
        assertEquals("$custom|6|10", b.text("SELECT libraryExerciseId || '|' || targetRepsMin || '|' || targetReps FROM exercise_definitions WHERE id = 'x'"))
        assertEquals("the built-in never left A", 0L, b.count("SELECT count(*) FROM exercises WHERE source = 'BUILTIN'"))

        // Deleting it on A deletes it on B
        a.execSQL("DELETE FROM exercise_names WHERE exerciseId = '$custom'")
        a.execSQL("DELETE FROM exercise_muscles WHERE exerciseId = '$custom'")
        a.execSQL("DELETE FROM exercises WHERE id = '$custom'")
        SyncPull(b).applyPage(serverItemsFromA().filter { it.entity == "library_exercise" }, 2)
        assertEquals(0L, b.count("SELECT count(*) FROM exercises"))
        assertEquals(0L, b.count("SELECT count(*) FROM exercise_muscles"))
    }

    @Test
    fun aSessionInTheOldShapeGetsTheTypeItsScreenMeant() {
        fun old(type: String, at: Long) = IncomingChange(
            "session", java.util.UUID.randomUUID().toString(), 1_000, false,
            buildJsonObject {
                put("workoutId", JsonNull); put("startedAt", at); put("finishedAt", at + 1); put("type", type); put("distanceMeters", 10.0)
            },
        )
        SyncPull(b).applyPage(listOf(old("RUN", 1), old("WALK", 2)), 1)
        assertEquals("RUNNING|run,WALKING|walk", b.text(
            "SELECT group_concat(t) FROM (SELECT type || '|' || activityTypeId AS t FROM sessions ORDER BY startedAt)"
        ))
        assertEquals("0", b.text("SELECT max(loggedManually) FROM sessions"))
    }
}
