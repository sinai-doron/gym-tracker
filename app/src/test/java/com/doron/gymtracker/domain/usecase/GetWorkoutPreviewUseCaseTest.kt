package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.seed.TrainingPlanData
import com.doron.gymtracker.domain.model.Session
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GetWorkoutPreviewUseCaseTest {

    private lateinit var sessionRepo: SessionRepository
    private lateinit var getLastWeight: GetLastWeightUseCase
    private lateinit var useCase: GetWorkoutPreviewUseCase
    private val workout = TrainingPlanData.workoutA

    @Before
    fun setup() {
        sessionRepo = mockk()
        getLastWeight = mockk()
        val resolveVariant = mockk<ResolveExerciseVariantUseCase>()
        every { resolveVariant(any(), any()) } answers { firstArg() }
        coEvery { getLastWeight(any()) } returns null
        useCase = GetWorkoutPreviewUseCase(sessionRepo, getLastWeight, resolveVariant)
    }

    @Test
    fun `never done workout has no history`() = runTest {
        coEvery { sessionRepo.getCompletedSince(0) } returns emptyList()
        val preview = useCase(workout)
        assertNull(preview.lastDoneAt)
        assertNull(preview.typicalMinutes)
        assertEquals(workout.exercises.size, preview.exercises.size)
    }

    @Test
    fun `uses only this workout's gym sessions for history`() = runTest {
        val min = 60_000L
        coEvery { sessionRepo.getCompletedSince(0) } returns listOf(
            Session(4L, "workout_b", 9_000 * min, 9_090 * min),
            Session(3L, workout.id, 5_000 * min, 5_050 * min),
            Session(2L, "", 4_000 * min, 4_030 * min, type = "RUNNING"),
            Session(1L, workout.id, 1_000 * min, 1_060 * min)
        )
        val preview = useCase(workout)
        assertEquals(5_000 * min, preview.lastDoneAt)
        assertEquals(55, preview.typicalMinutes)
    }

    @Test
    fun `fills last weight per exercise`() = runTest {
        coEvery { sessionRepo.getCompletedSince(0) } returns emptyList()
        coEvery { getLastWeight(workout.exercises.first().id) } returns 60f
        val preview = useCase(workout)
        assertEquals(60f, preview.exercises.first().lastWeightKg)
        assertNull(preview.exercises.last().lastWeightKg)
    }
}
