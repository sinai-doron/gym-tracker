package com.doron.gymtracker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.doron.gymtracker.data.db.GymDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs up everything the app stores — the Room database and the DataStore preferences
 * (rotation, settings, step baseline) — to a single zip the user saves anywhere, and
 * restores from one. After a restore the app must restart, since the database is closed.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: GymDatabase
) {
    sealed interface RestoreResult {
        data object Success : RestoreResult
        data object NotABackup : RestoreResult
        data object FromNewerVersion : RestoreResult
    }

    private val dbFile get() = context.getDatabasePath(DB_NAME)
    private val datastoreDir get() = File(context.filesDir, "datastore")

    suspend fun export(target: Uri) = withContext(Dispatchers.IO) {
        // Fold the write-ahead log into the main file so the single .db file is complete
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        context.contentResolver.openOutputStream(target)!!.use { out ->
            ZipOutputStream(out).use { zip ->
                zip.add(DB_ENTRY, dbFile)
                datastoreDir.listFiles()?.filter { it.isFile }?.forEach { zip.add("$DATASTORE_PREFIX${it.name}", it) }
            }
        }
    }

    suspend fun restore(source: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "restore").apply { deleteRecursively(); mkdirs() }
        try {
            context.contentResolver.openInputStream(source)!!.use { input ->
                ZipInputStream(input).use { zip ->
                    generateSequence { zip.nextEntry }.forEach { entry ->
                        // Only the files a backup contains; ignore anything else (and any path tricks)
                        val name = entry.name
                        val allowed = name == DB_ENTRY ||
                            (name.startsWith(DATASTORE_PREFIX) && !name.removePrefix(DATASTORE_PREFIX).contains('/'))
                        if (allowed && !entry.isDirectory) {
                            File(staging, name).apply { parentFile?.mkdirs() }.outputStream().use { zip.copyTo(it) }
                        }
                    }
                }
            }
            val stagedDb = File(staging, DB_ENTRY)
            if (!stagedDb.exists()) return@withContext RestoreResult.NotABackup
            val version = try {
                SQLiteDatabase.openDatabase(stagedDb.path, null, SQLiteDatabase.OPEN_READONLY).use { backup ->
                    backup.rawQuery("SELECT 1 FROM sessions LIMIT 1", null).close()
                    backup.version
                }
            } catch (e: Exception) {
                return@withContext RestoreResult.NotABackup
            }
            // Room can migrate an older backup forward, but can't open one from a newer app
            if (version > db.openHelper.readableDatabase.version) return@withContext RestoreResult.FromNewerVersion

            db.close()
            listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm")).forEach { it.delete() }
            stagedDb.copyTo(dbFile, overwrite = true)
            File(staging, DATASTORE_PREFIX.trimEnd('/')).listFiles()?.forEach { prefs ->
                prefs.copyTo(File(datastoreDir.apply { mkdirs() }, prefs.name), overwrite = true)
            }
            RestoreResult.Success
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun ZipOutputStream.add(name: String, file: File) {
        putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private companion object {
        const val DB_NAME = "gym_tracker.db"
        const val DB_ENTRY = "gym_tracker.db"
        const val DATASTORE_PREFIX = "datastore/"
    }
}
