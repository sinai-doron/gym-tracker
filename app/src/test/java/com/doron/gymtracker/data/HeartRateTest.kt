package com.doron.gymtracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeartRateTest {

    private val start = 1_000_000L
    private val end = start + 10 * 60_000L
    // 1 km in 10 minutes, steadily
    private val marks = (0..10).map { DistanceMark(start + it * 60_000L, it * 100.0) }

    private fun at(minute: Double, bpm: Int) = HeartRateSample(start + (minute * 60_000).toLong(), bpm)

    @Test
    fun `no readings during the run, no heart rate`() {
        assertNull(HeartRate.summary(emptyList(), start, end, marks))
        // Before and after the run only
        assertNull(HeartRate.summary(listOf(HeartRateSample(start - 1, 120), HeartRateSample(end + 1, 130)), start, end, marks))
    }

    @Test
    fun `the average weighs each reading by how long it lasted`() {
        // 100 for 9 minutes, then 190 for the last minute: a reading every 5 s at the end
        // must not outvote the long steady stretch
        val samples = listOf(at(0.0, 100)) + (0 until 12).map { at(9.0 + it / 12.0, 190) }
        val summary = HeartRate.summary(samples, start, end, marks)!!
        assertEquals(190, summary.maxBpm)
        // 100 counts for a minute at most (a longer gap is the watch not reading): (100 + 190) / 2
        assertEquals(145, summary.avgBpm)
    }

    @Test
    fun `implausible readings are left out`() {
        val summary = HeartRate.summary(listOf(at(1.0, 140), at(2.0, 300), at(3.0, 10)), start, end, marks)!!
        assertEquals(140, summary.maxBpm)
        assertEquals(140, summary.avgBpm)
    }

    @Test
    fun `the chart puts readings at how far the run had gone, a point a minute`() {
        val samples = listOf(at(2.0, 120), at(2.5, 130), at(5.0, 150))
        val profile = HeartRate.summary(samples, start, end, marks)!!.profile
        assertEquals(listOf(200.0 to 125.0, 500.0 to 150.0), profile)
    }

    @Test
    fun `distance at a time is in proportion between marks`() {
        assertEquals(0.0, HeartRate.metersAt(marks, start - 5), 0.001)
        assertEquals(150.0, HeartRate.metersAt(marks, start + 90_000), 0.001)
        assertEquals(1000.0, HeartRate.metersAt(marks, end + 60_000), 0.001)
        assertEquals(0.0, HeartRate.metersAt(emptyList(), start), 0.001)
    }
}
