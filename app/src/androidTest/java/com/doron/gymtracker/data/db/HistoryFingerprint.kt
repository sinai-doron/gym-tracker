package com.doron.gymtracker.data.db

import android.database.Cursor

/**
 * What a history adds up to, read with plain SQL so it works on any schema from version 10 on:
 * the numbers a migration must leave exactly as they were. Two fingerprints, one taken before a
 * migration and one after, must be equal.
 *
 * The v14 migration moves sets into `set_parts` (ADR-0006); it adds a second way to read the same
 * numbers from v14 on, so before and after stay comparable.
 */
data class HistoryFingerprint(
    val sessionsByType: Map<String, Long>,
    val finishedSessions: Long,
    val setsByType: Map<String, Long>,
    val totalReps: Long,
    /** Sum of reps × weight in grams, so a Float round-trip can't make two equal histories differ. */
    val volumeGrams: Long,
    val bestPerExercise: Map<String, Best>,
    val runDistances: Map<Long, Double?>,
    val gpsPointsPerSession: Map<Long, Long>,
    val pauses: Long,
    val routes: Long,
    val routePoints: Long,
    val songs: Long,
    val workouts: Map<String, String>,
    /** Each exercise as name|type|sets|reps|distance|rest|bodyweight|variantOf|removed. */
    val exercises: Map<String, String>,
) {
    data class Best(val weightKg: Double?, val reps: Long?, val durationSecs: Long?, val distanceM: Long?)

    /** A few lines for the test log, so a run on real data shows what it compared. */
    fun summary(): String =
        "sessions $sessionsByType ($finishedSessions finished), sets $setsByType, reps $totalReps, " +
            "volume ${volumeGrams / 1000} kg, exercises with sets ${bestPerExercise.size}, " +
            "runs ${runDistances.size}, GPS points ${gpsPointsPerSession.values.sum()}, routes $routes"

    companion object {
        /** [query] runs one SELECT; pass the framework's `rawQuery` or Room's `query`. */
        fun read(query: (String) -> Cursor): HistoryFingerprint = HistoryFingerprint(
            sessionsByType = query.map("SELECT type, count(*) FROM sessions GROUP BY type") { it.getString(0) to it.getLong(1) },
            finishedSessions = query.long("SELECT count(*) FROM sessions WHERE finishedAt IS NOT NULL"),
            setsByType = query.map("SELECT type, count(*) FROM set_records GROUP BY type") { it.getString(0) to it.getLong(1) },
            totalReps = query.long("SELECT coalesce(sum(reps), 0) FROM set_records"),
            volumeGrams = query.long(
                "SELECT coalesce(sum(round(reps * weightKg * 1000)), 0) FROM set_records " +
                    "WHERE reps IS NOT NULL AND weightKg IS NOT NULL"
            ),
            bestPerExercise = query.map(
                "SELECT e.exerciseId, max(s.weightKg), max(s.reps), max(s.durationSecs), max(s.distanceM) " +
                    "FROM set_records s JOIN exercise_entries e ON e.id = s.entryId GROUP BY e.exerciseId"
            ) { it.getString(0) to Best(it.doubleOrNull(1), it.longOrNull(2), it.longOrNull(3), it.longOrNull(4)) },
            runDistances = query.map("SELECT id, distanceMeters FROM sessions WHERE type IN ('RUNNING', 'WALKING')") {
                it.getLong(0) to it.doubleOrNull(1)
            },
            gpsPointsPerSession = query.map("SELECT sessionId, count(*) FROM location_log GROUP BY sessionId") {
                it.getLong(0) to it.getLong(1)
            },
            pauses = query.long("SELECT count(*) FROM run_pauses"),
            routes = query.long("SELECT count(*) FROM routes"),
            routePoints = query.long("SELECT count(*) FROM route_points"),
            songs = query.long("SELECT count(*) FROM track_log"),
            workouts = query.map("SELECT id, label FROM workouts") { it.getString(0) to it.getString(1) },
            exercises = query.map(
                "SELECT id, name || '|' || type || '|' || targetSets || '|' || coalesce(targetReps, '') || '|' || " +
                    "coalesce(targetDistanceM, '') || '|' || restDurationSecs || '|' || isBodyweight || '|' || " +
                    "coalesce(variantOfId, '') || '|' || isRemoved FROM exercise_definitions"
            ) { it.getString(0) to it.getString(1) },
        )

        private fun <K, V> ((String) -> Cursor).map(sql: String, row: (Cursor) -> Pair<K, V>): Map<K, V> =
            this(sql).use { c -> buildMap { while (c.moveToNext()) row(c).let { (k, v) -> put(k, v) } } }

        private fun ((String) -> Cursor).long(sql: String): Long =
            this(sql).use { it.moveToFirst(); it.getLong(0) }

        private fun Cursor.doubleOrNull(i: Int): Double? = if (isNull(i)) null else getDouble(i)
        private fun Cursor.longOrNull(i: Int): Long? = if (isNull(i)) null else getLong(i)
    }
}
