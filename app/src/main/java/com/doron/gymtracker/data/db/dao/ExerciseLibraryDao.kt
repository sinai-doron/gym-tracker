package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.doron.gymtracker.data.db.entities.ExerciseEntity
import com.doron.gymtracker.data.db.entities.ExerciseMuscleEntity
import com.doron.gymtracker.data.db.entities.ExerciseNameEntity

@Dao
interface ExerciseLibraryDao {
    /** The catalogue version the built-ins were last written from; null before the first seed. */
    @Query("SELECT max(catalogueVersion) FROM exercises WHERE source = 'BUILTIN'")
    suspend fun builtinVersion(): Int?

    @Query("SELECT count(*) FROM exercises WHERE source = 'BUILTIN' AND archivedAt IS NULL")
    suspend fun builtinCount(): Int

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun get(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE slug = :slug")
    suspend fun getBySlug(slug: String): ExerciseEntity?

    @Query("SELECT * FROM exercise_names WHERE exerciseId = :id")
    suspend fun names(id: String): List<ExerciseNameEntity>

    @Query("SELECT * FROM exercise_names WHERE exerciseId IN (:ids)")
    suspend fun namesFor(ids: List<String>): List<ExerciseNameEntity>

    @Query("SELECT * FROM exercise_muscles WHERE exerciseId = :id")
    suspend fun muscles(id: String): List<ExerciseMuscleEntity>

    /** Exercises whose name or alias, in any language, contains [text]; archived ones left out. */
    @Query("""
        SELECT * FROM exercises WHERE archivedAt IS NULL AND id IN
            (SELECT exerciseId FROM exercise_names WHERE name LIKE '%' || :text || '%')
    """)
    suspend fun search(text: String): List<ExerciseEntity>

    /** Every exercise not archived, built-in or the user's own. */
    @Query("SELECT * FROM exercises WHERE archivedAt IS NULL")
    suspend fun allActive(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_names")
    suspend fun allNames(): List<ExerciseNameEntity>

    @Query("SELECT * FROM exercise_muscles")
    suspend fun allMuscles(): List<ExerciseMuscleEntity>

    /** Library exercises in history, the most recently done first. */
    @Query(
        """SELECT e.libraryExerciseId FROM exercise_entries e JOIN sessions s ON s.id = e.sessionId
           WHERE e.libraryExerciseId IS NOT NULL GROUP BY e.libraryExerciseId ORDER BY max(s.startedAt) DESC LIMIT :limit"""
    )
    suspend fun recentlyDone(limit: Int): List<String>

    /** A user's own exercise with its names and muscles, in one go. */
    @Transaction
    suspend fun insertCustom(exercise: ExerciseEntity, names: List<ExerciseNameEntity>, muscles: List<ExerciseMuscleEntity>) {
        upsertExercises(listOf(exercise))
        insertNames(names)
        insertMuscles(muscles)
    }

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercise_names WHERE exerciseId IN (SELECT id FROM exercises WHERE source = 'BUILTIN')")
    suspend fun deleteBuiltinNames()

    @Query("DELETE FROM exercise_muscles WHERE exerciseId IN (SELECT id FROM exercises WHERE source = 'BUILTIN')")
    suspend fun deleteBuiltinMuscles()

    @Insert
    suspend fun insertNames(names: List<ExerciseNameEntity>)

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Query("UPDATE exercises SET archivedAt = :now WHERE source = 'BUILTIN' AND archivedAt IS NULL AND id NOT IN (:keep)")
    suspend fun archiveBuiltinsExcept(keep: List<String>, now: Long)

    /**
     * Writes the catalogue's built-ins over the ones the library has: new ones added, changed ones
     * updated, ones gone from the catalogue archived (history still points at them). A user's own
     * exercises are never touched.
     */
    @Transaction
    suspend fun replaceBuiltins(
        exercises: List<ExerciseEntity>,
        names: List<ExerciseNameEntity>,
        muscles: List<ExerciseMuscleEntity>,
        now: Long
    ) {
        archiveBuiltinsExcept(exercises.map { it.id }, now)
        upsertExercises(exercises)
        deleteBuiltinNames()
        deleteBuiltinMuscles()
        insertNames(names)
        insertMuscles(muscles)
    }

    /** Links an old plan exercise to the library, once; a link already made is left alone. */
    @Query("UPDATE exercise_definitions SET libraryExerciseId = :exerciseId WHERE id = :planId AND libraryExerciseId IS NULL")
    suspend fun link(planId: String, exerciseId: String)

    /** And the history logged under it, for entries made or downloaded before the link. */
    @Query("UPDATE exercise_entries SET libraryExerciseId = :exerciseId WHERE exerciseId = :planId AND libraryExerciseId IS NULL")
    suspend fun linkEntries(planId: String, exerciseId: String)

    @Transaction
    suspend fun linkAll(links: Map<String, String>) {
        links.forEach { (planId, exerciseId) ->
            link(planId, exerciseId)
            linkEntries(planId, exerciseId)
        }
    }
}
