package com.doron.gymtracker.data.repository

import com.doron.gymtracker.data.db.dao.ActivityTypeDao
import com.doron.gymtracker.data.db.entities.ActivityTypeEntity
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTypesTest {

    private val dao = mockk<ActivityTypeDao>(relaxed = true)
    private val types = TrainingTypes(dao)

    private fun own(id: String) = ActivityTypeEntity(id, "Football", "football", "#000000", "DURATION", builtin = false)

    @Test
    fun `the first-release types arrive hidden, once`() = runTest {
        types.ensureBuiltins(now = 42)
        types.ensureBuiltins(now = 43)
        coVerify(exactly = 1) {
            dao.insertMissing(match { rows ->
                rows.map { it.id }.containsAll(listOf("pilates", "yoga", "martial_arts", "swimming", "hiit", "team_sports", "mobility", "other")) &&
                    rows.all { it.hiddenAt == 42L && it.builtin && it.recorder == "DURATION" && it.healthConnectType != null && it.metValue != null }
            })
        }
    }

    @Test
    fun `every icon and colour is one the server takes`() {
        TrainingTypes.HIDDEN_BUILTINS.forEach {
            assertTrue(it.icon, it.icon.matches(Regex("^[a-z0-9_]{1,40}$")))
            assertTrue(it.color, it.color.matches(Regex("^#[0-9A-Fa-f]{6}$")))
            assertTrue(it.id, it.metValue!! in 0.0..30.0)
        }
    }

    @Test
    fun `strength can't be hidden`() = runTest {
        types.setShown("strength", false)
        coVerify(exactly = 0) { dao.setHidden(any(), any()) }
        types.setShown("run", false)
        coVerify { dao.setHidden("run", match { it != null }) }
        types.setShown("run", true)
        coVerify { dao.setHidden("run", null) }
    }

    @Test
    fun `an unused type of the user's own is deleted, a used one hidden`() = runTest {
        coEvery { dao.get("a") } returns own("a")
        coEvery { dao.sessionCount("a") } returns 0
        types.remove("a")
        coVerify { dao.deleteOwn("a") }

        coEvery { dao.get("b") } returns own("b")
        coEvery { dao.sessionCount("b") } returns 3
        types.remove("b")
        coVerify(exactly = 0) { dao.deleteOwn("b") }
        coVerify { dao.setHidden("b", match { it != null }) }
    }

    @Test
    fun `a built-in is only ever hidden`() = runTest {
        coEvery { dao.get("yoga") } returns TrainingTypes.HIDDEN_BUILTINS.first { it.id == "yoga" }
        coEvery { dao.sessionCount("yoga") } returns 0
        types.remove("yoga")
        coVerify(exactly = 0) { dao.deleteOwn(any()) }
        coVerify { dao.setHidden("yoga", match { it != null }) }
    }

    @Test
    fun `calories are MET times kg times hours`() {
        val pilates = TrainingType("pilates", null, "pilates", "#E17055", Recorder.DURATION, 48, 3.0, true, 10, false)
        assertEquals(234, pilates.estimateCalories(3600, 78f))
        assertEquals(117, pilates.estimateCalories(1800, 78f))
        assertNull(pilates.estimateCalories(3600, null))
        assertNull(pilates.copy(metValue = null).estimateCalories(3600, 78f))
    }

    @Test
    fun `only types that cover ground ask for a distance`() {
        fun type(id: String, recorder: Recorder) = TrainingType(id, null, id, "#000000", recorder, null, null, true, 0, false)
        assertTrue(type("run", Recorder.GPS).hasDistance)
        assertTrue(type("swimming", Recorder.DURATION).hasDistance)
        assertTrue(type("cardio_machines", Recorder.DURATION).hasDistance)
        assertEquals(false, type("pilates", Recorder.DURATION).hasDistance)
        assertEquals(false, type("strength", Recorder.EXERCISES).hasDistance)
    }
}
