package com.doron.gymtracker.data.repository

import android.content.Context
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.db.dao.ExerciseLibraryDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The exercise library: the catalogue's built-ins, kept in the database so they can be searched and joined. */
@Singleton
class ExerciseLibrary @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ExerciseLibraryDao
) {
    private val seedLock = Mutex()
    @Volatile private var seeded = false

    /**
     * Writes the built-ins when the app's catalogue differs from the library's (a fresh install, or
     * an update that ships a new catalogue), then links the plan's exercises to them. Run after the
     * plan is seeded, so a fresh install's plan rows exist to link.
     */
    suspend fun ensureSeeded() {
        if (seeded) return
        seedLock.withLock {
            if (seeded) return
            withContext(Dispatchers.IO) {
                val catalogue = ExerciseCatalogue.parse(context.assets.open(ExerciseCatalogue.ASSET).bufferedReader().use { it.readText() })
                seed(dao, catalogue, System.currentTimeMillis())
            }
            seeded = true
        }
    }

    companion object {
        internal suspend fun seed(dao: ExerciseLibraryDao, catalogue: ExerciseCatalogue, now: Long) {
            if (dao.builtinVersion() != catalogue.version || dao.builtinCount() != catalogue.exercises.size) {
                val (exercises, names, muscles) = catalogue.toEntities()
                dao.replaceBuiltins(exercises, names, muscles, now)
            }
            dao.linkAll(catalogue.legacyIds)
        }
    }
}
