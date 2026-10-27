package com.doron.gymtracker.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

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
    }

    private val client: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    suspend fun hasPermissions(): Boolean {
        val c = client ?: return false
        val granted = c.permissionController.getGrantedPermissions()
        return PERMISSIONS.all { it in granted }
    }

    suspend fun writeWorkoutSession(startedAt: Long, finishedAt: Long, title: String, sessionType: String = "GYM") {
        val c = client ?: return
        if (!hasPermissions()) return
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

    suspend fun readTodaySteps(): Long {
        val c = client ?: return 0
        if (!hasPermissions()) return 0
        return try {
            val now = Instant.now()
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
            val response = c.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            Log.w("HealthConnect", "Failed to read steps", e)
            0
        }
    }
}
