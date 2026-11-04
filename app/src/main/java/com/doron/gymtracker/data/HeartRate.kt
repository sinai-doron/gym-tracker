package com.doron.gymtracker.data

import kotlin.math.roundToInt

/** One heart rate reading, from a watch through Health Connect. */
data class HeartRateSample(val timestamp: Long, val bpm: Int)

/** A run's heart rate: its average and highest, and the readings against distance for a chart. */
data class HeartRateSummary(
    val avgBpm: Int,
    val maxBpm: Int,
    /** (meters along the run, bpm), one per minute at most, in order. */
    val profile: List<Pair<Double, Double>>
)

/**
 * A run's heart rate from the readings a watch took during it. Read from Health Connect each
 * time a run is shown and never stored, so it stays on the phone like all Health Connect data.
 */
object HeartRate {

    /**
     * The readings between [startedAt] and [finishedAt], set against how far the run had gone
     * ([marks]). Null with no usable readings: the run summary then leaves heart rate out.
     */
    fun summary(samples: List<HeartRateSample>, startedAt: Long, finishedAt: Long, marks: List<DistanceMark>): HeartRateSummary? {
        val during = samples
            .filter { it.timestamp in startedAt..finishedAt && it.bpm in PLAUSIBLE_BPM }
            .sortedBy { it.timestamp }
        if (during.isEmpty()) return null
        return HeartRateSummary(
            avgBpm = timeWeightedAverage(during, finishedAt),
            maxBpm = during.maxOf { it.bpm },
            profile = profile(during, startedAt, marks)
        )
    }

    /**
     * Each reading counts for as long as it lasted until the next one, so a watch that reads
     * every second while running hard and every minute while walking doesn't skew the average.
     */
    private fun timeWeightedAverage(samples: List<HeartRateSample>, finishedAt: Long): Int {
        if (samples.size == 1) return samples.first().bpm
        var weighted = 0.0
        var total = 0.0
        samples.forEachIndexed { i, s ->
            val until = samples.getOrNull(i + 1)?.timestamp ?: finishedAt
            // A gap longer than this is the watch not reading, not a steady heart rate
            val lasted = (until - s.timestamp).coerceIn(1, MAX_SAMPLE_MS).toDouble()
            weighted += s.bpm * lasted
            total += lasted
        }
        return (weighted / total).roundToInt()
    }

    /** The readings against distance, averaged by minute of the run so a reading a second draws cleanly. */
    private fun profile(samples: List<HeartRateSample>, startedAt: Long, marks: List<DistanceMark>): List<Pair<Double, Double>> =
        samples
            .groupBy { (it.timestamp - startedAt) / BUCKET_MS }
            .values
            .map { bucket -> metersAt(marks, bucket.first().timestamp) to bucket.map { it.bpm }.average() }

    /** How far the run had gone at [time]: between two marks, in proportion. */
    fun metersAt(marks: List<DistanceMark>, time: Long): Double {
        if (marks.isEmpty() || time < marks.first().timestamp) return 0.0
        if (time == marks.first().timestamp) return marks.first().meters
        val after = marks.indexOfFirst { it.timestamp >= time }
        if (after == -1) return marks.last().meters
        val a = marks[after - 1]
        val b = marks[after]
        val f = (time - a.timestamp).toDouble() / (b.timestamp - a.timestamp).coerceAtLeast(1)
        return a.meters + (b.meters - a.meters) * f
    }

    /** Readings outside this are a watch that slipped, not a heart. */
    private val PLAUSIBLE_BPM = 30..240
    private const val MAX_SAMPLE_MS = 60_000L
    private const val BUCKET_MS = 60_000L
}
