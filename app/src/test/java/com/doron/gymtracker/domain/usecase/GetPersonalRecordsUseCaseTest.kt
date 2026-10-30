package com.doron.gymtracker.domain.usecase

import com.doron.gymtracker.data.BestEffort
import com.doron.gymtracker.data.DistanceMark
import com.doron.gymtracker.domain.model.RecordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetPersonalRecordsUseCaseTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `finds the fastest stretch inside a longer run`() {
        // 3 km: 1 km at 6:00, 1 km at 4:30, 1 km at 6:00, a mark every 10 m
        val secsPer10m = listOf(3.6, 2.7, 3.6)
        val marks = mutableListOf(DistanceMark(0, 0.0))
        var t = 0.0
        for (km in 0..2) for (i in 1..100) {
            t += secsPer10m[km]
            marks += DistanceMark((t * 1000).toLong(), km * 1000 + i * 10.0)
        }
        val active = marks.map { it.timestamp }
        assertEquals(270.0, BestEffort.fastestSecs(marks, active, 1_000.0)!!, 0.5)
        // The whole run, start to end
        assertEquals(990.0, BestEffort.fastestSecs(marks, active, 3_000.0)!!, 0.5)
        assertNull(BestEffort.fastestSecs(marks, active, 5_000.0))
    }

    @Test
    fun `picks the best of each kind`() {
        fun day(d: Int) = LocalDate.of(2026, 10, d).atStartOfDay(zone).toInstant().toEpochMilli()
        val runs = listOf(
            RunFacts(1, day(19), 5_300.0, mapOf(RecordKind.FASTEST_1K to 290.0, RecordKind.FASTEST_5K to 1_600.0)),
            RunFacts(2, day(21), 3_000.0, mapOf(RecordKind.FASTEST_1K to 280.0)),
            // A week later: one 8 km run, less than the 8.3 km of the week before
            RunFacts(3, day(27), 8_000.0, mapOf(RecordKind.FASTEST_1K to 300.0, RecordKind.FASTEST_5K to 1_650.0))
        )
        val records = GetPersonalRecordsUseCase.records(runs, zone).associateBy { it.kind }
        assertEquals(2L, records[RecordKind.FASTEST_1K]!!.sessionId)
        assertEquals(1L, records[RecordKind.FASTEST_5K]!!.sessionId)
        assertNull(records[RecordKind.FASTEST_10K])
        assertEquals(3L, records[RecordKind.LONGEST_RUN]!!.sessionId)
        // Sunday 18 Oct's week: 5.3 + 3 km
        assertEquals(8_300.0, records[RecordKind.BEST_WEEK]!!.value, 0.1)
        assertEquals(day(18), records[RecordKind.BEST_WEEK]!!.at)
    }
}
