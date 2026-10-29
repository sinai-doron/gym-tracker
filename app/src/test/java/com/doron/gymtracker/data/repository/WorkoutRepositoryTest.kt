package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.WorkoutDao
import com.doron.gymtracker.data.db.entities.ExerciseDefinitionEntity
import com.doron.gymtracker.data.db.entities.WorkoutEntity
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.RepExerciseDefinition
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

    private fun row(id: String, order: Int, variantOf: String? = null) =
        ExerciseDefinitionEntity(id, "workout_a", id, "REP", 3, 90, 10, null, variantOfId = variantOf, sortOrder = order)

    @Test
    fun `moveExercise swaps neighbours and renumbers`() = runTest {
        coEvery { dao.getExercisesForWorkout("workout_a") } returns
            listOf(row("e1", 1), row("e2", 2), row("e2b", 0, variantOf = "e2"), row("e3", 3))
        repo.moveExercise("workout_a", "e2", -1)
        coVerify { dao.setSortOrder("e2", 1) }
        coVerify { dao.setSortOrder("e1", 2) }
        coVerify { dao.setSortOrder("e3", 3) }
        coVerify(exactly = 0) { dao.setSortOrder("e2b", any()) }
    }

    @Test
    fun `moveExercise past either end does nothing`() = runTest {
        coEvery { dao.getExercisesForWorkout("workout_a") } returns listOf(row("e1", 1), row("e2", 2))
        repo.moveExercise("workout_a", "e1", -1)
        repo.moveExercise("workout_a", "e2", 1)
        coVerify(exactly = 0) { dao.setSortOrder(any(), any()) }
    }

    @Test
    fun `updateExercise keeps workout, position and type`() = runTest {
        coEvery { dao.getExercise("e1") } returns row("e1", 4)
        repo.updateExercise(RepExerciseDefinition("e1", "New name", 5, 120, 8, isBodyweight = true))
        coVerify {
            dao.updateExercise(match {
                it.name == "New name" && it.targetSets == 5 && it.restDurationSecs == 120 &&
                    it.targetReps == 8 && it.isBodyweight && it.workoutId == "workout_a" &&
                    it.sortOrder == 4 && it.type == "REP"
            })
        }
    }

    @Test
    fun `addExercise goes to the end of the workout`() = runTest {
        coEvery { dao.maxSortOrder("workout_a") } returns 7
        repo.addExercise("workout_a", RepExerciseDefinition("custom_1", "Dips", 3, 90, 10))
        coVerify { dao.insertExercise(match { it.id == "custom_1" && it.sortOrder == 8 }) }
    }
}
