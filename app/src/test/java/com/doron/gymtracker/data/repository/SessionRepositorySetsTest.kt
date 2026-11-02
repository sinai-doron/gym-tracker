package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ExerciseEntryDao
import com.doron.gymtracker.data.db.dao.SetDao
import com.doron.gymtracker.data.db.dao.SetWithParts
import com.doron.gymtracker.data.db.entities.ExerciseEntryEntity
import com.doron.gymtracker.data.db.entities.SetEntity
import com.doron.gymtracker.data.db.entities.SetPartEntity
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TimedSetRecord
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sets as the screens know them (one row of numbers) over sets with parts (ADR-0006). */
class SessionRepositorySetsTest {

    private val entries = mockk<ExerciseEntryDao>(relaxed = true)
    private val sets = mockk<SetDao>(relaxed = true)
    private val repo = SessionRepositoryImpl(mockk(relaxed = true), entries, sets, mockk(), mockk(), mockk())

    @Test
    fun `a set is logged as one part, and 0 kg as no load`() = runTest {
        coEvery { entries.getForSessionAndExercise(1, "d2_e8a") } returns ExerciseEntryEntity(7, 1, "d2_e8a")

        repo.addSetRecord(1, "d2_e8a", RepSetRecord(0, 0, 500, reps = 12, weightKg = 0f))
        repo.addSetRecord(1, "d2_e8a", RepSetRecord(0, 0, 600, reps = 8, weightKg = 62.5f))

        coVerify { sets.insert(7, 500, listOf(SetPartEntity(0, 1, reps = 12, loadKg = null))) }
        coVerify { sets.insert(7, 600, listOf(SetPartEntity(0, 1, reps = 8, loadKg = 62.5f))) }
    }

    @Test
    fun `an edit rewrites the set's parts`() = runTest {
        repo.updateSetRecord(TimedSetRecord(3, 7, 900, durationSecs = 50))
        coVerify { sets.update(3, 900, listOf(SetPartEntity(0, 1, durationS = 50))) }
    }

    @Test
    fun `parts read back as the set the screens know`() = runTest {
        coEvery { entries.getForSession(1) } returns listOf(ExerciseEntryEntity(7, 1, "x"))
        coEvery { sets.getForEntry(7) } returns listOf(
            SetWithParts(SetEntity(1, 7, 1, completedAt = 100), listOf(SetPartEntity(1, 1, reps = 8, loadKg = 60f))),
            SetWithParts(SetEntity(2, 7, 2, completedAt = 200), listOf(SetPartEntity(2, 1, reps = 12))),
            SetWithParts(SetEntity(3, 7, 3, completedAt = 300), listOf(SetPartEntity(3, 1, durationS = 45))),
            SetWithParts(SetEntity(4, 7, 4, completedAt = 400), listOf(SetPartEntity(4, 1, distanceM = 35))),
        )

        assertEquals(
            listOf(
                RepSetRecord(1, 7, 100, 8, 60f),
                RepSetRecord(2, 7, 200, 12, 0f),
                TimedSetRecord(3, 7, 300, 45),
                DistanceSetRecord(4, 7, 400, 35),
            ),
            repo.getEntriesForSession(1).single().sets
        )
    }
}
