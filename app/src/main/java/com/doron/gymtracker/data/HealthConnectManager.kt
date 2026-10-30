package com.doron.gymtracker.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(ExerciseSessionRecord::class)
        )

        /** What a run adds beyond the session itself. Each is optional: a run saves what's allowed. */
        val RUN_PERMISSIONS = setOf(
            HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE,
            HealthPermission.getWritePermission(DistanceRecord::class),
            HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class),
            HealthPermission.getWritePermission(ElevationGainedRecord::class),
            // A watch's heart rate during runs
            HealthPermission.getReadPermission(HeartRateRecord::class)
        )

        val ALL_PERMISSIONS = PERMISSIONS + RUN_PERMISSIONS
    }

    private val client: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(): Boolean = granted().containsAll(PERMISSIONS)

    suspend fun hasAllPermissions(): Boolean = granted().containsAll(ALL_PERMISSIONS)

    private suspend fun granted(): Set<String> =
        client?.permissionController?.getGrantedPermissions() ?: emptySet()

    suspend fun writeWorkoutSession(startedAt: Long, finishedAt: Long, title: String, sessionType: String = "GYM") {
        val c = client ?: return
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted()) return
        try {
            val exerciseType = when (sessionType) {
                "RUNNING" -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                "WALKING" -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
                else -> ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING
            }
            val record = ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(startedAt),
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(startedAt)),
                endTime = Instant.ofEpochMilli(finishedAt),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(finishedAt)),
                metadata = Metadata.manualEntry(),
                exerciseType = exerciseType,
                title = title
            )
            c.insertRecords(listOf(record))
        } catch (e: Exception) {
            Log.w("HealthConnect", "Failed to write workout", e)
        }
    }

    /**
     * Saves a run or walk: the session with its laps and route, plus distance, active
     * calories and climb. Steps are left out: the phone already reports them.
     */
    suspend fun writeRun(run: RunRecord) {
        val c = client ?: return
        val granted = granted()
        if (HealthPermission.getWritePermission(ExerciseSessionRecord::class) !in granted) return
        val start = Instant.ofEpochMilli(run.startedAt)
        val end = Instant.ofEpochMilli(run.finishedAt)
        val startOffset = ZoneId.systemDefault().rules.getOffset(start)
        val endOffset = ZoneId.systemDefault().rules.getOffset(end)
        fun allowed(record: KClass<out Record>) = HealthPermission.getWritePermission(record) in granted
        try {
            val route = run.route.filter { it.timestamp in run.startedAt..run.finishedAt }
            val session = ExerciseSessionRecord(
                startTime = start, startZoneOffset = startOffset,
                endTime = end, endZoneOffset = endOffset,
                metadata = Metadata.activelyRecorded(Device(type = Device.TYPE_PHONE)),
                exerciseType = if (run.type == "WALKING") ExerciseSessionRecord.EXERCISE_TYPE_WALKING
                    else ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
                title = if (run.type == "WALKING") "הליכה" else "ריצה",
                laps = run.laps.withStarts(run.startedAt).map { (lap, lapStart) ->
                    ExerciseLap(Instant.ofEpochMilli(lapStart), Instant.ofEpochMilli(lap.endedAt), Length.meters(lap.distanceMeters))
                },
                exerciseRoute = if (HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE in granted && route.size >= 2) {
                    ExerciseRoute(route.map {
                        ExerciseRoute.Location(
                            time = Instant.ofEpochMilli(it.timestamp),
                            latitude = it.latitude,
                            longitude = it.longitude,
                            horizontalAccuracy = it.accuracy?.let { a -> Length.meters(a.toDouble()) },
                            altitude = it.altitude?.let(Length::meters)
                        )
                    })
                } else null
            )
            val records = buildList<Record> {
                add(session)
                if (allowed(DistanceRecord::class) && run.distanceMeters > 0) {
                    add(DistanceRecord(start, startOffset, end, endOffset, Length.meters(run.distanceMeters), Metadata.activelyRecorded(Device(type = Device.TYPE_PHONE))))
                }
                if (allowed(ActiveCaloriesBurnedRecord::class) && (run.calories ?: 0) > 0) {
                    add(ActiveCaloriesBurnedRecord(start, startOffset, end, endOffset, Energy.kilocalories(run.calories!!.toDouble()), Metadata.activelyRecorded(Device(type = Device.TYPE_PHONE))))
                }
                if (allowed(ElevationGainedRecord::class) && run.elevationGainM > 0) {
                    add(ElevationGainedRecord(start, startOffset, end, endOffset, Length.meters(run.elevationGainM), Metadata.activelyRecorded(Device(type = Device.TYPE_PHONE))))
                }
            }
            c.insertRecords(records)
        } catch (e: Exception) {
            Log.w("HealthConnect", "Failed to write run", e)
        }
    }

    suspend fun readTodaySteps(): Long {
        val c = client ?: return 0
        if (HealthPermission.getReadPermission(StepsRecord::class) !in granted()) return 0
        return try {
            val now = Instant.now()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            // aggregate() de-duplicates steps written by several apps (e.g. phone + watch);
            // summing raw records double-counts them and is capped at one page of records
            val response = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0
        } catch (e: Exception) {
            Log.w("HealthConnect", "Failed to read steps", e)
            0
        }
    }

    /**
     * Steps on each of [days] days from [from], de-duplicated across apps like today's count.
     * Days without any data are missing; null when steps can't be read at all.
     */
    suspend fun readDailySteps(from: LocalDate, days: Int): Map<LocalDate, Long>? {
        val c = client ?: return null
        if (HealthPermission.getReadPermission(StepsRecord::class) !in granted()) return null
        return try {
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), from.plusDays(days.toLong()).atStartOfDay()),
                    timeRangeSlicer = Period.ofDays(1)
                )
            ).associate { it.startTime.toLocalDate() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L) }
        } catch (e: Exception) {
            Log.w("HealthConnect", "Failed to read daily steps", e)
            null
        }
    }
}

/** A finished run or walk, as Health Connect is told about it. */
data class RunRecord(
    val type: String,
    val startedAt: Long,
    val finishedAt: Long,
    val distanceMeters: Double,
    val elevationGainM: Double,
    val calories: Int?,
    val laps: List<Lap>,
    val route: List<LocationPoint>
)

/** Each lap paired with when it started: the run's start, then the previous lap's end. */
private fun List<Lap>.withStarts(runStart: Long): List<Pair<Lap, Long>> =
    mapIndexed { i, lap -> lap to (if (i == 0) runStart else this[i - 1].endedAt) }
