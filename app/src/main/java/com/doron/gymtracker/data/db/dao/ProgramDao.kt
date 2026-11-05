package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.ProgramEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE isActive = 1 ORDER BY rowid LIMIT 1")
    suspend fun getActive(): ProgramEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(program: ProgramEntity)

    /** Every programme, the active one first, then by when it was made. */
    @Query("SELECT * FROM programs ORDER BY isActive DESC, rowid")
    suspend fun getAll(): List<ProgramEntity>

    @Query("SELECT * FROM programs WHERE id = :id")
    suspend fun get(id: String): ProgramEntity?

    @Query("UPDATE programs SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE programs SET startEpochDay = :epochDay WHERE id = :id")
    suspend fun setStart(id: String, epochDay: Long)

    @Query(
        "UPDATE programs SET deloadEveryWeeks = :deloadEveryWeeks, deloadLoadPercent = :deloadLoadPercent, " +
            "deloadSets = :deloadSets, variantRule = :variantRule, variantEveryWeeks = :variantEveryWeeks WHERE id = :id"
    )
    suspend fun setRules(
        id: String, deloadEveryWeeks: Int?, deloadLoadPercent: Int, deloadSets: Int, variantRule: String, variantEveryWeeks: Int
    )

    /** Makes [id] the only active programme; an archived one comes back with it. */
    @Query("UPDATE programs SET isActive = (id = :id), archivedAt = CASE WHEN id = :id THEN NULL ELSE archivedAt END")
    suspend fun makeActive(id: String)

    @Query("UPDATE programs SET archivedAt = :at WHERE id = :id AND isActive = 0")
    suspend fun setArchived(id: String, at: Long?)

    @Query("DELETE FROM programs WHERE id = :id")
    suspend fun delete(id: String)

    /** Sessions done in the programme's workouts, hidden ones included. */
    @Query("SELECT count(*) FROM sessions s JOIN workouts w ON w.id = s.workoutId WHERE w.programId = :id")
    suspend fun sessionCount(id: String): Int

    @Insert
    suspend fun insertWorkouts(workouts: List<WorkoutEntity>)

    @Insert
    suspend fun insertExercises(exercises: List<ExerciseDefinitionEntity>)

    /** A whole programme at once, made the active one: all of it is written or none. */
    @Transaction
    suspend fun install(program: ProgramEntity, workouts: List<WorkoutEntity>, exercises: List<ExerciseDefinitionEntity>) {
        insert(program)
        insertWorkouts(workouts)
        insertExercises(exercises)
        makeActive(program.id)
    }

    /** The programme's workouts that are in its rotation. */
    @Query("SELECT count(*) FROM workouts WHERE programId = :id AND hiddenAt IS NULL")
    suspend fun workoutCount(id: String): Int
}
