package com.doron.gymtracker.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.LocationLogEntity
import com.doron.gymtracker.data.db.entities.SessionEntity
import com.doron.gymtracker.data.db.entities.SetRecordEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The path the owner's phone took: a version 10 database with history, opened by the current app.
 * Version 10's schema was never exported, so it is written out here from the entities as they were
 * at that version (git: the parent of da5b172), in the form Room creates tables. Room then runs
 * the real migrations and checks the result against today's entities; it throws if they differ.
 *
 * Later versions are tested against the schemas Room exports to app/schemas/.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom10Test {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var room: GymDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(NAME)
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(NAME).apply { parentFile?.mkdirs() }, null).use { db ->
            (VERSION_10_SCHEMA + SAMPLE_HISTORY).forEach(db::execSQL)
            db.version = 10
        }
    }

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(NAME)
    }

    private fun fingerprintBefore(): HistoryFingerprint =
        SQLiteDatabase.openDatabase(context.getDatabasePath(NAME).path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            HistoryFingerprint.read { sql -> db.rawQuery(sql, null) }
        }

    /** Opens the file the way the app does, which runs the migrations to the current version. */
    private fun migrate(): GymDatabase =
        Room.databaseBuilder(context, GymDatabase::class.java, NAME)
            .addMigrations(*GymDatabase.MIGRATIONS)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = SyncMigration.installFresh(db)
            })
            .allowMainThreadQueries()
            .build()
            .also { room = it; it.openHelper.writableDatabase }

    private val sql get() = room!!.openHelper.writableDatabase

    @Test
    fun historyIsUnchanged() {
        val before = fingerprintBefore()
        migrate()
        assertEquals(before, HistoryFingerprint.read { sql.query(it) })
    }

    @Test
    fun everyRowGetsAUuidAndIsQueuedForUploadOnce() {
        migrate()
        assertEquals(emptyMap<String, Long>(), SyncChecks.rowsWithoutUuid(sql))
        assertEquals(SyncChecks.expectedChanges(sql), SyncChecks.loggedChanges(sql))
        assertEquals("not left in download mode", 0L, SyncChecks.applying(sql))
    }

    @Test
    fun triggersAreTheOnesAFreshInstallGets() {
        migrate()
        assertEquals(SyncChecks.freshInstallTriggers(context), SyncChecks.triggers(sql))
    }

    /** Version 14 links each plan exercise to its built-in; history and sync are untouched. */
    @Test
    fun thePlanIsLinkedToTheLibrary() {
        migrate()
        val links = sql.query("SELECT id, libraryExerciseId FROM exercise_definitions").use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }
        assertEquals(
            mapOf(
                "d1_e1" to "barbell-bench-press", "d1_e7" to "plank", "d2_e8a" to "crunch",
                "d2_e8b" to "hanging-knee-raise", "d2_e6" to "hip-adduction-machine", "d3_e8" to "farmers-walk",
            ).mapValues { (_, slug) -> ExerciseCatalogue.builtinId(slug) },
            links
        )
    }

    /** The v11/v12 triggers broke exactly this: the app's own writes, through Room, after the migration. */
    @Test
    fun theAppCanWriteAfterwards() = runTest {
        val db = migrate()
        val before = SyncChecks.loggedChanges(sql)

        val run = db.sessionDao().insert(SessionEntity(0, "RUNNING", 50_000, null, "RUNNING"))
        repeat(3) { i -> db.locationLogDao().insert(LocationLogEntity(sessionId = run, timestamp = 50_000L + i, latitude = 32.1, longitude = 34.8, speed = null, altitude = null)) }
        db.sessionDao().update(db.sessionDao().getById(run)!!.copy(finishedAt = 60_000, distanceMeters = 20.0))

        val gym = db.sessionDao().insert(SessionEntity(0, "workout_a", 70_000, null, "GYM"))
        val entry = db.exerciseEntryDao().insert(ExerciseEntryEntity(0, gym, "d1_e1"))
        val set = SetRecordEntity(db.setRecordDao().insert(SetRecordEntity(0, entry, 71_000, "REP", 8, 60f, null, null)), entry, 71_000, "REP", 8, 60f, null, null)
        db.setRecordDao().update(set.copy(reps = 9))
        db.setRecordDao().update(set.copy(reps = 10))

        val after = SyncChecks.loggedChanges(sql)
        assertEquals(before.getValue("session") + 2, after.getValue("session"))
        assertEquals(before.getValue("track") + 1, after.getValue("track"))
        assertEquals(before.getValue("entry") + 1, after.getValue("entry"))
        assertEquals(before.getValue("set") + 1, after.getValue("set"))
        assertEquals(emptyMap<String, Long>(), SyncChecks.rowsWithoutUuid(sql))
    }

    private companion object {
        const val NAME = "migration-from-10-test.db"

        /** The tables at version 10, as Room created them from that version's entities. */
        val VERSION_10_SCHEMA = listOf(
            "CREATE TABLE IF NOT EXISTS `workouts` (`id` TEXT NOT NULL, `label` TEXT NOT NULL, PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `exercise_definitions` (`id` TEXT NOT NULL, `workoutId` TEXT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `targetSets` INTEGER NOT NULL, `restDurationSecs` INTEGER NOT NULL, " +
                "`targetReps` INTEGER, `targetDistanceM` INTEGER, `isOptional` INTEGER NOT NULL, `isPerSide` INTEGER NOT NULL, " +
                "`isBodyweight` INTEGER NOT NULL, `variantOfId` TEXT, `sortOrder` INTEGER NOT NULL, `isRemoved` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
            "CREATE TABLE IF NOT EXISTS `sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workoutId` TEXT NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `finishedAt` INTEGER, `type` TEXT NOT NULL, `distanceMeters` REAL)",
            "CREATE TABLE IF NOT EXISTS `exercise_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`sessionId` INTEGER NOT NULL, `exerciseId` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `set_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, " +
                "`completedAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `reps` INTEGER, `weightKg` REAL, `durationSecs` INTEGER, " +
                "`distanceM` INTEGER)",
            "CREATE TABLE IF NOT EXISTS `track_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `location_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sessionId` INTEGER NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `speed` REAL, `altitude` REAL, " +
                "`accuracy` REAL, `bearing` REAL)",
            "CREATE TABLE IF NOT EXISTS `run_details` (`sessionId` INTEGER NOT NULL, `targetType` TEXT NOT NULL, `targetValue` REAL, " +
                "`lapMeters` INTEGER NOT NULL, `stepCounterAtStart` INTEGER, `steps` INTEGER, `elevationGainM` REAL, `calories` INTEGER, " +
                "`weatherTempC` INTEGER, `weatherCondition` TEXT, PRIMARY KEY(`sessionId`))",
            "CREATE TABLE IF NOT EXISTS `routes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`distanceMeters` REAL NOT NULL, `createdAt` INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS `route_points` (`routeId` INTEGER NOT NULL, `seq` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                "`longitude` REAL NOT NULL, `elevationM` REAL, PRIMARY KEY(`routeId`, `seq`))",
            "CREATE INDEX IF NOT EXISTS `index_route_points_routeId` ON `route_points` (`routeId`)",
            "CREATE TABLE IF NOT EXISTS `run_pauses` (`sessionId` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER, " +
                "PRIMARY KEY(`sessionId`, `startedAt`))",
        )

        /**
         * A small history with one of everything: rep, bodyweight (saved as 0 kg), timed and distance
         * sets, a week-B variant, a removed exercise, an unfinished session, a run with GPS points,
         * details and a pause, a walk, a route with points, and a song.
         */
        val SAMPLE_HISTORY = listOf(
            "INSERT INTO workouts VALUES ('workout_a', 'A'), ('workout_b', 'B'), ('workout_c', 'C')",
            "INSERT INTO exercise_definitions VALUES " +
                "('d1_e1', 'workout_a', 'לחיצת חזה עם מוט (שטוח)', 'REP', 4, 180, 6, NULL, 0, 0, 0, NULL, 1, 0), " +
                "('d1_e7', 'workout_a', 'פלאנק', 'TIMED', 3, 60, NULL, NULL, 0, 0, 0, NULL, 7, 0), " +
                "('d2_e8a', 'workout_b', 'כפיפות בטן', 'REP', 3, 60, 12, NULL, 0, 0, 1, NULL, 8, 0), " +
                "('d2_e8b', 'workout_b', 'הרמות ברכיים בתלייה', 'REP', 3, 60, 12, NULL, 0, 0, 1, 'd2_e8a', 0, 0), " +
                "('d2_e6', 'workout_b', 'קירוב ירכיים במכונה', 'REP', 3, 60, 15, NULL, 1, 0, 0, NULL, 6, 1), " +
                "('d3_e8', 'workout_c', 'הליכת איכר', 'DISTANCE', 3, 90, NULL, 35, 0, 0, 0, NULL, 8, 0)",
            "INSERT INTO sessions VALUES " +
                "(1, 'workout_a', 1000, 5000, 'GYM', NULL), " +
                "(2, 'workout_b', 6000, NULL, 'GYM', NULL), " +
                "(3, 'RUNNING', 10000, 20000, 'RUNNING', 5012.5), " +
                "(4, 'WALKING', 30000, 40000, 'WALKING', 2100.0)",
            "INSERT INTO exercise_entries VALUES (1, 1, 'd1_e1'), (2, 1, 'd1_e7'), (3, 1, 'd3_e8'), (4, 2, 'd2_e8a')",
            "INSERT INTO set_records VALUES " +
                "(1, 1, 1100, 'REP', 8, 60.0, NULL, NULL), " +
                "(2, 1, 1200, 'REP', 6, 62.5, NULL, NULL), " +
                "(3, 2, 1300, 'TIMED', NULL, NULL, 45, NULL), " +
                "(4, 3, 1400, 'DISTANCE', NULL, NULL, NULL, 35), " +
                "(5, 4, 6100, 'REP', 15, 0.0, NULL, NULL)",
            "INSERT INTO location_log VALUES " +
                "(1, 3, 10000, 32.0853, 34.7818, 2.9, 12.0, 5.0, 90.0), " +
                "(2, 3, 11000, 32.0860, 34.7825, 3.1, 13.5, 4.0, 91.0), " +
                "(3, 3, 12000, 32.0868, 34.7831, 3.0, NULL, NULL, NULL)",
            "INSERT INTO run_details VALUES (3, 'DISTANCE', 5000.0, 1000, 123456, 6400, 35.5, 410, 22, 'CLEAR')",
            "INSERT INTO run_pauses VALUES (3, 15000, 15500)",
            "INSERT INTO routes VALUES (1, 'פארק הירקון', 5012.5, 20000)",
            "INSERT INTO route_points VALUES (1, 0, 32.0853, 34.7818, 12.0), (1, 1, 32.0868, 34.7831, NULL)",
            "INSERT INTO track_log VALUES (1, 1, 1150, 'Song', 'Artist')",
        )
    }
}
