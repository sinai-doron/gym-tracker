package com.doron.gymtracker.data.repository

import android.content.Context
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.data.catalogue.LibraryVocabulary
import com.doron.gymtracker.data.db.dao.ExerciseLibraryDao
import com.doron.gymtracker.data.db.entities.ExerciseEntity
import com.doron.gymtracker.data.db.entities.ExerciseMuscleEntity
import com.doron.gymtracker.data.db.entities.ExerciseNameEntity
import com.doron.gymtracker.domain.model.LibraryExercise
import com.doron.gymtracker.domain.model.TargetDefaults
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
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
    /** Each built-in's default targets, from the catalogue file (the table doesn't hold them). */
    @Volatile private var defaultsById: Map<String, TargetDefaults> = emptyMap()

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
                defaultsById = catalogue.exercises.associate { e ->
                    ExerciseCatalogue.builtinId(e.slug) to e.defaults.let {
                        TargetDefaults(it.sets, it.repsMin, it.repsMax, it.restS, it.durationS, it.distanceM)
                    }
                }
            }
            seeded = true
        }
    }

    /** Every exercise not archived, named in [lang] (a language code: en, he). */
    suspend fun all(lang: String): List<LibraryExercise> {
        ensureSeeded()
        return withContext(Dispatchers.IO) {
            val names = dao.allNames().groupBy { it.exerciseId }
            val muscles = dao.allMuscles().groupBy { it.exerciseId }
            dao.allActive().map { e ->
                val own = names[e.id].orEmpty()
                val proper = own.filter { !it.isAlias }
                val name = (proper.firstOrNull { it.lang == lang } ?: proper.firstOrNull { it.lang == "en" } ?: own.firstOrNull())?.name ?: e.id
                LibraryExercise(
                    id = e.id,
                    name = name,
                    searchNames = own.map { it.name },
                    preset = e.preset,
                    equipment = e.equipment,
                    primaryMuscles = muscles[e.id].orEmpty().filter { it.isPrimary }.map { it.muscle },
                    secondaryMuscles = muscles[e.id].orEmpty().filter { !it.isPrimary }.map { it.muscle },
                    perSide = e.sides == "per_side",
                    isCustom = e.source == CUSTOM,
                    defaults = defaultsById[e.id] ?: CUSTOM_DEFAULTS,
                )
            }
        }
    }

    /** Library exercises in history, the most recently done first. */
    suspend fun recentlyDone(limit: Int = 10): List<String> = withContext(Dispatchers.IO) { dao.recentlyDone(limit) }

    /**
     * A user's own exercise: [name] in [lang], what a set records, its equipment and main muscle.
     * Saved as CUSTOM, so it syncs and the catalogue never overwrites it. Returns its id.
     */
    suspend fun createCustom(name: String, lang: String, preset: String, equipment: String, muscle: String): String {
        val id = UUID.randomUUID().toString()
        val exercise = ExerciseEntity(
            id = id, slug = null, source = CUSTOM, preset = preset, load = loadFor(preset), sides = "both",
            equipment = equipment, mechanics = null,
            weightStepKg = LibraryVocabulary.EQUIPMENT_STEP_KG[equipment].takeIf { loadFor(preset) != "none" },
            catalogueVersion = null
        )
        withContext(Dispatchers.IO) {
            dao.insertCustom(exercise, listOf(ExerciseNameEntity(id, lang, name.trim(), isAlias = false)), listOf(ExerciseMuscleEntity(id, muscle, isPrimary = true)))
        }
        return id
    }

    companion object {
        const val CUSTOM = "CUSTOM"
        private val CUSTOM_DEFAULTS = TargetDefaults(sets = 3, repsMin = 8, repsMax = 12, restS = 90, durationS = 45, distanceM = 30)

        /** How a preset's load counts (ADR-0006): reps with a weight count the weight moved. */
        fun loadFor(preset: String): String = when (preset) {
            "reps_load", "distance_load" -> "total"
            "reps_assisted" -> "assistance"
            else -> "none"
        }

        internal suspend fun seed(dao: ExerciseLibraryDao, catalogue: ExerciseCatalogue, now: Long) {
            if (dao.builtinVersion() != catalogue.version || dao.builtinCount() != catalogue.exercises.size) {
                val (exercises, names, muscles) = catalogue.toEntities()
                dao.replaceBuiltins(exercises, names, muscles, now)
            }
            dao.linkAll(catalogue.legacyIds)
        }
    }
}
