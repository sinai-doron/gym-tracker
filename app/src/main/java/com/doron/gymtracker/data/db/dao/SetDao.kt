package com.doron.gymtracker.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.doron.gymtracker.data.db.entities.SetEntity
import com.doron.gymtracker.data.db.entities.SetPartEntity

data class SetWithParts(
    @Embedded val set: SetEntity,
    @Relation(parentColumn = "id", entityColumn = "setId")
    val parts: List<SetPartEntity>
)

data class SetWithSession(
    @Embedded val set: SetEntity,
    val sessionId: Long,
    val sessionStartedAt: Long,
    @Relation(parentColumn = "id", entityColumn = "setId")
    val parts: List<SetPartEntity>
)

@Dao
interface SetDao {
    @Insert
    suspend fun insertSet(set: SetEntity): Long

    @Insert
    suspend fun insertParts(parts: List<SetPartEntity>)

    @Query("SELECT coalesce(max(seq), 0) + 1 FROM sets WHERE entryId = :entryId")
    suspend fun nextSeq(entryId: Long): Int

    /** Adds a set at the end of its exercise; [parts] get the new set's id. */
    @Transaction
    suspend fun insert(entryId: Long, completedAt: Long, parts: List<SetPartEntity>): Long {
        val id = insertSet(SetEntity(0, entryId, nextSeq(entryId), completedAt = completedAt))
        insertParts(parts.mapIndexed { i, p -> p.copy(setId = id, seq = i + 1) })
        return id
    }

    @Query("UPDATE sets SET completedAt = :completedAt WHERE id = :id")
    suspend fun updateCompletedAt(id: Long, completedAt: Long)

    @Query("DELETE FROM set_parts WHERE setId = :setId")
    suspend fun deleteParts(setId: Long)

    /** Rewrites what a set recorded; its position, kind and note stay. */
    @Transaction
    suspend fun update(id: Long, completedAt: Long, parts: List<SetPartEntity>) {
        updateCompletedAt(id, completedAt)
        deleteParts(id)
        insertParts(parts.mapIndexed { i, p -> p.copy(setId = id, seq = i + 1) })
    }

    @Transaction
    @Query("SELECT * FROM sets WHERE entryId = :entryId ORDER BY seq")
    suspend fun getForEntry(entryId: Long): List<SetWithParts>

    @Transaction
    @Query("SELECT s.* FROM sets s JOIN exercise_entries e ON s.entryId = e.id WHERE e.sessionId = :sessionId")
    suspend fun getForSession(sessionId: Long): List<SetWithParts>

    /**
     * Every set ever logged in a finished session for the plan exercise [exerciseId], oldest first:
     * under that plan row, or under any row of the same library exercise (its week-B twin, another
     * workout or programme, the plan before an edit), which the entry names too.
     */
    @Transaction
    @Query("""
        SELECT st.*, s.id AS sessionId, s.startedAt AS sessionStartedAt FROM sets st
        JOIN exercise_entries ee ON st.entryId = ee.id
        JOIN sessions s ON s.id = ee.sessionId
        WHERE s.finishedAt IS NOT NULL AND (
            ee.exerciseId = :exerciseId OR
            ee.libraryExerciseId = (SELECT libraryExerciseId FROM exercise_definitions WHERE id = :exerciseId)
        )
        ORDER BY s.startedAt, st.completedAt
    """)
    suspend fun getHistoryForExercise(exerciseId: String): List<SetWithSession>

    @Query("DELETE FROM sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Transaction
    suspend fun deleteById(id: Long) {
        deleteParts(id)
        deleteSet(id)
    }

    @Query("DELETE FROM set_parts WHERE setId IN (SELECT st.id FROM sets st JOIN exercise_entries e ON st.entryId = e.id WHERE e.sessionId = :sessionId)")
    suspend fun deletePartsForSession(sessionId: Long)

    @Query("DELETE FROM sets WHERE entryId IN (SELECT id FROM exercise_entries WHERE sessionId = :sessionId)")
    suspend fun deleteSetsForSession(sessionId: Long)

    @Transaction
    suspend fun deleteForSession(sessionId: Long) {
        deletePartsForSession(sessionId)
        deleteSetsForSession(sessionId)
    }

    @Query("""
        DELETE FROM set_parts WHERE setId IN (SELECT st.id FROM sets st JOIN exercise_entries e ON st.entryId = e.id
            WHERE e.sessionId = :sessionId AND e.exerciseId = :exerciseId)
    """)
    suspend fun deletePartsForExercise(sessionId: Long, exerciseId: String)

    @Query("DELETE FROM sets WHERE entryId IN (SELECT id FROM exercise_entries WHERE sessionId = :sessionId AND exerciseId = :exerciseId)")
    suspend fun deleteSetsForExercise(sessionId: Long, exerciseId: String)

    @Transaction
    suspend fun deleteForExercise(sessionId: Long, exerciseId: String) {
        deletePartsForExercise(sessionId, exerciseId)
        deleteSetsForExercise(sessionId, exerciseId)
    }
}
