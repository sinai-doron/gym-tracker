package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.EntryNote
import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.ExerciseNoteDao
import com.doron.gymtracker.data.db.entities.ExerciseNoteEntity
import com.doron.gymtracker.sync.SyncPayloads
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/**
 * What a user writes about an exercise and whether they do it today: the note kept on the
 * exercise (seat height, grip), a note on one session's sets, and skipping it for one session.
 */
class ExerciseNotes @Inject constructor(
    private val entryDao: ExerciseEntryDao,
    private val noteDao: ExerciseNoteDao
) {
    /** The library exercise a plan exercise is; null for one from before the library. */
    suspend fun libraryIdOf(planExerciseId: String): String? = entryDao.libraryIdOf(planExerciseId)

    fun kept(libraryExerciseId: String?): Flow<String?> =
        libraryExerciseId?.let(noteDao::observe) ?: flowOf(null)

    /** Saves the note kept on an exercise; a blank one removes it. */
    suspend fun keep(libraryExerciseId: String, text: String) {
        val trimmed = text.trim().take(SyncPayloads.EXERCISE_NOTE_MAX)
        if (trimmed.isEmpty()) noteDao.delete(libraryExerciseId)
        else noteDao.upsert(ExerciseNoteEntity(libraryExerciseId, trimmed))
    }

    /** The newest note on this exercise's sets from an earlier session, for "last time". */
    suspend fun lastTime(libraryExerciseId: String?, sessionId: Long): EntryNote? =
        libraryExerciseId?.let { entryDao.lastNote(it, sessionId) }

    /** Saves the note on this session's sets; a blank one clears it. */
    suspend fun noteToday(sessionId: Long, planExerciseId: String, text: String) {
        val entry = entryDao.getOrCreate(sessionId, planExerciseId)
        entryDao.setNote(entry.id, text.trim().take(SyncPayloads.NOTE_MAX).ifEmpty { null })
    }

    suspend fun skip(sessionId: Long, planExerciseId: String, skipped: Boolean) {
        entryDao.setSkipped(entryDao.getOrCreate(sessionId, planExerciseId).id, skipped)
    }
}
