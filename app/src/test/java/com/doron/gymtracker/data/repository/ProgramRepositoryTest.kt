package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ProgramDao
import com.doron.gymtracker.data.db.entities.ProgramEntity
import com.doron.gymtracker.domain.model.Workout
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ProgramRepositoryTest {

    private lateinit var dao: ProgramDao
    private lateinit var workouts: WorkoutRepository
    private lateinit var repo: ProgramRepository

    private fun program(id: String, active: Boolean = false, archivedAt: Long? = null) =
        ProgramEntity(id, "P $id", LocalDate.of(2026, 11, 2).toEpochDay(), null, "NONE", isActive = active, archivedAt = archivedAt)

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        workouts = mockk(relaxed = true)
        coEvery { dao.getAll() } returns listOf(program("program_abc", active = true), program("mine"), program("done"))
        coEvery { dao.sessionCount(any()) } returns 0
        coEvery { dao.sessionCount("done") } returns 4
        coEvery { dao.workoutCount(any()) } returns 3
        repo = ProgramRepositoryImpl(dao, workouts)
    }

    @Test
    fun `the list says which programmes can be deleted`() = runTest {
        val byId = repo.getAll().associateBy { it.program.id }
        assertFalse("the active one", byId.getValue("program_abc").canDelete)
        assertTrue("one of the user's own, never done", byId.getValue("mine").canDelete)
        assertFalse("one with sessions", byId.getValue("done").canDelete)
        assertEquals(4, byId.getValue("done").sessionCount)
    }

    @Test
    fun `a new programme starts empty and inactive`() = runTest {
        val id = repo.create("Upper / Lower", LocalDate.of(2026, 11, 2))
        coVerify {
            dao.insert(match {
                it.id == id && it.name == "Upper / Lower" && !it.isActive && it.deloadEveryWeeks == null &&
                    it.variantRule == "NONE" && it.startEpochDay == LocalDate.of(2026, 11, 2).toEpochDay()
            })
        }
    }

    @Test
    fun `deleting takes its workouts with it`() = runTest {
        coEvery { workouts.getForProgram("mine") } returns listOf(Workout("w1", "A", emptyList()), Workout("w2", "B", emptyList()))
        assertTrue(repo.delete("mine"))
        coVerify { workouts.removeWorkout("w1") }
        coVerify { workouts.removeWorkout("w2") }
        coVerify { dao.delete("mine") }
    }

    @Test
    fun `a programme with history, the seeded one or the active one is never deleted`() = runTest {
        assertFalse(repo.delete("done"))
        assertFalse(repo.delete("program_abc"))
        coVerify(exactly = 0) { dao.delete(any()) }
    }
}
