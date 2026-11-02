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
                override fun onCreate(db: SupportSQLiteDatabase) = SyncMigration.installFresh(db)
            })
            .build()
            .also { room = it }
        val sql = db.openHelper.writableDatabase

        val after = HistoryFingerprint.read { sql.query(it) }
        Log.i(TAG, "after migrating to version ${sql.version}: ${after.summary()}")
        assertEquals(before, after)
        assertEquals(emptyMap<String, Long>(), SyncChecks.rowsWithoutUuid(sql))
        assertEquals(SyncChecks.freshInstallTriggers(context), SyncChecks.triggers(sql))
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
