package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutRepositoryTest {

    private lateinit var dao: WorkoutDao
    private lateinit var repo: WorkoutRepository

    @Before
    fun setup() {
        dao = mockk(relaxed = true)
        repo = WorkoutRepositoryImpl(dao)
    }

    @Test
    fun `ensureSeeded inserts the whole plan including week-B variants`() = runTest {
        repo.ensureSeeded()
        coVerify(exactly = 1) {
            dao.seed(
                match { it.size == 3 },
                match { rows ->
                    rows.any { it.id == "d1_e1" && it.variantOfId == null } &&
                        rows.any { it.id == "d1_e3b" && it.variantOfId == "d1_e3a" }
                }
            )
        }
    }

    @Test
    fun `ensureSeeded runs once per app start`() = runTest {
        repo.ensureSeeded()
        repo.ensureSeeded()
        coVerify(exactly = 1) { dao.seed(any(), any()) }
    }

    @Test
    fun `getAll seeds before reading`() = runTest {
        repo.getAll()
        coVerify(exactly = 1) { dao.seed(any(), any()) }
    }

    @Test
    fun `getAll attaches variant rows to their week-A exercise`() = runTest {
        coEvery { dao.getAll() } returns listOf(WorkoutEntity("workout_a", "A"))
        coEvery { dao.getExercisesForWorkout("workout_a") } returns listOf(
            ExerciseDefinitionEntity("d1_e3a", "workout_a", "A", "REP", 3, 90, 10, null),
            ExerciseDefinitionEntity("d1_e3b", "workout_a", "B", "REP", 3, 90, 10, null, variantOfId = "d1_e3a")
        )
        val exercises = repo.getAll().single().exercises
        assertEquals(listOf("d1_e3a"), exercises.map { it.id })
        assertEquals("d1_e3b", exercises.single().variantB?.id)
    }

    @Test
    fun `getAll maps all 3 workouts from DB`() = runTest {
        val seededEntities = TrainingPlanData.all.map { WorkoutEntity(it.id, it.label.name) }
        coEvery { dao.getAll() } returns seededEntities
        coEvery { dao.getExercisesForWorkout(any()) } returns emptyList()
        val all = repo.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun `getById returns null for unknown id`() = runTest {
        val seededEntities = TrainingPlanData.all.map { WorkoutEntity(it.id, it.label.name) }
        coEvery { dao.getAll() } returns seededEntities
        coEvery { dao.getExercisesForWorkout(any()) } returns emptyList()
        assertNull(repo.getById("nonexistent"))
    }
}
