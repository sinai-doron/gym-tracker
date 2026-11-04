package com.doron.gymtracker.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Real history through the real migrations: a backup zip from Settings → Backup, opened by the
 * current app, must add up to the same numbers before and after. Skipped when no backup is on the
 * device; docs/testing/database-migrations.md says how to put one there.
 *
 * Run it on an emulator, never on the phone the backup came from: connectedAndroidTest uninstalls
 * the app when it finishes, and that deletes the app's data.
 */
@RunWith(AndroidJUnit4::class)
class BackupMigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var room: GymDatabase? = null

    @After
    fun tearDown() {
        room?.close()
        context.deleteDatabase(NAME)
    }

    @Test
    fun aRealBackupKeepsItsHistory() {
        val zip = backupZip()
        assumeTrue("no backup at ${zip.path}; see docs/testing/database-migrations.md", zip.exists())

        context.deleteDatabase(NAME)
        val file = context.getDatabasePath(NAME).apply { parentFile?.mkdirs() }
        ZipInputStream(zip.inputStream()).use { entries ->
            val db = generateSequence { entries.nextEntry }.firstOrNull { it.name == DB_ENTRY }
            assertNotNull("$DB_ENTRY is not in ${zip.name}", db)
            file.outputStream().use { entries.copyTo(it) }
        }

        val (version, before) = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.version to HistoryFingerprint.read { sql -> db.rawQuery(sql, null) }
        }
        Log.i(TAG, "backup at version $version: ${before.summary()}")

        val db = Room.databaseBuilder(context, GymDatabase::class.java, NAME)
            .addMigrations(*GymDatabase.MIGRATIONS)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) = GymDatabase.installFresh(db)
            })
            .build()
            .also { room = it }
        val sql = db.openHelper.writableDatabase

        val after = HistoryFingerprint.read { sql.query(it) }
        Log.i(TAG, "after migrating to version ${sql.version}: ${after.summary()}")
        assertEquals(before, after)
        assertEquals(emptyMap<String, Long>(), SyncChecks.rowsWithoutUuid(sql))
        assertEquals(SyncChecks.freshInstallTriggers(context), SyncChecks.triggers(sql))

        // Version 14: every plan exercise the owner's plan has is linked to its built-in
        val unlinked = sql.query(
            "SELECT id FROM exercise_definitions WHERE libraryExerciseId IS NULL AND id IN " +
                LibraryMigration.PLAN_TO_BUILTIN.keys.joinToString(", ", "(", ")") { "'$it'" }
        ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
        assertEquals(emptyList<String>(), unlinked)

        // Version 15: every set is queued, so the server gets them in the new shape
        assertEquals(SyncChecks.expectedChanges(sql)["set"], SyncChecks.loggedChanges(sql)["set"])

        // Version 16: one programme; the workouts rotate A, B, C; every entry of a linked exercise points at the library
        assertEquals(1L, sql.query("SELECT count(*) FROM programs WHERE isActive = 1").use { it.moveToFirst(); it.getLong(0) })
        val order = sql.query("SELECT label FROM workouts WHERE programId = 'program_abc' ORDER BY sortOrder").use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0)) }
        }
        assertEquals(listOf("A", "B", "C"), order)
        val unlinkedEntries = sql.query(
            "SELECT count(*) FROM exercise_entries e JOIN exercise_definitions d ON d.id = e.exerciseId " +
                "WHERE d.libraryExerciseId IS NOT NULL AND (e.libraryExerciseId IS NULL OR e.libraryExerciseId != d.libraryExerciseId)"
        ).use { it.moveToFirst(); it.getLong(0) }
        assertEquals(0L, unlinkedEntries)
        assertEquals(SyncChecks.expectedChanges(sql)["entry"], SyncChecks.loggedChanges(sql)["entry"])

        // Version 17: every session names the training type its screen meant, and is queued with it
        val mistyped = sql.query(
            "SELECT count(*) FROM sessions WHERE activityTypeId != " +
                "CASE type WHEN 'RUNNING' THEN 'run' WHEN 'WALKING' THEN 'walk' ELSE 'strength' END"
        ).use { it.moveToFirst(); it.getLong(0) }
        assertEquals(0L, mistyped)
        assertEquals(SyncChecks.expectedChanges(sql)["session"], SyncChecks.loggedChanges(sql)["session"])
        assertEquals(3L, SyncChecks.loggedChanges(sql)["activity_type"])
    }

    /** The `backupZip` instrumentation argument, or gym-backup.zip in the app's external files folder. */
    private fun backupZip(): File =
        InstrumentationRegistry.getArguments().getString("backupZip")?.let(::File)
            ?: File(context.getExternalFilesDir(null), "gym-backup.zip")

    private companion object {
        const val TAG = "BackupMigrationTest"
        const val NAME = "backup-under-test.db"
        /** The database's entry in a backup zip (BackupManager.DB_ENTRY). */
        const val DB_ENTRY = "gym_tracker.db"
    }
}
