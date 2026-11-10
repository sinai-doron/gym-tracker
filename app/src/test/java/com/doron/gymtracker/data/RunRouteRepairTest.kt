package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RunRouteRepairTest {

    private fun points(n: Int, from: Long) = (0 until n).map { LocationPoint(32.5, 34.9, null, null, null, from + it * 2000L) }

    @Test
    fun `a run the sync cut to its first seconds gets its whole route back`() {
        val start = 1_794_251_619_959L
        val restore = RunRouteRepair.restorable(
            local = mapOf(start to (11L to 5)),
            recorded = listOf(RecordedRoute(start, points(1046, start)))
        )
        assertEquals(setOf(11L), restore.keys)
        assertEquals(1046, restore.getValue(11L).size)
    }

    @Test
    fun `a run with all its points is left alone`() {
        val start = 1_000L
        val restore = RunRouteRepair.restorable(
            local = mapOf(start to (3L to 2126)),
            // Health Connect gets the route between start and finish: never more than the phone kept
            recorded = listOf(RecordedRoute(start, points(2120, start)))
        )
        assertEquals(emptyMap<Long, List<LocationPoint>>(), restore)
    }

    @Test
    fun `a route matches only the run that started at the same moment`() {
        val restore = RunRouteRepair.restorable(
            local = mapOf(1_000L to (1L to 0), 9_000L to (2L to 0)),
            recorded = listOf(RecordedRoute(9_000L, points(10, 9_000L)), RecordedRoute(5_000L, points(10, 5_000L)))
        )
        assertEquals(setOf(2L), restore.keys)
    }
}
