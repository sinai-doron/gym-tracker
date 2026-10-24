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
    fun `ensureSeeded calls insertAll with 3 workouts when DB is empty`() = runTest {
        coEvery { dao.getAll() } returns emptyList()
        repo.ensureSeeded()
        coVerify(exactly = 1) { dao.insertAll(match { it.size == 3 }) }
    }

    @Test
    fun `ensureSeeded is idempotent — skips insert when rows exist`() = runTest {
        val seededEntities = TrainingPlanData.all.map { WorkoutEntity(it.id, it.label.name) }
        coEvery { dao.getAll() } returns seededEntities
        repo.ensureSeeded()
        coVerify(exactly = 0) { dao.insertAll(any()) }
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
