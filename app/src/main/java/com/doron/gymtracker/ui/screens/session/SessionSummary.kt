package com.doron.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen

data class SessionSummaryData(
    val workoutName: String,
    val durationSecs: Long,
    val exercisesCompleted: Int,
    val exercisesTotal: Int,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val maxWeightKg: Float,
    val exerciseBreakdown: List<ExerciseSummaryLine>,
    val tracks: List<TrackLog> = emptyList()
)

data class ExerciseSummaryLine(
    val exerciseId: String,
    val name: String,
    val setsCompleted: Int,
    val setsTarget: Int,
    val topWeight: Float?
)

fun buildSummaryData(
    workoutName: String,
    durationSecs: Long,
    detailStates: Map<String, ExerciseDetailUiState>,
    tracks: List<TrackLog> = emptyList()
): SessionSummaryData {
    var totalSets = 0
    var totalVolume = 0f
    var maxWeight = 0f
    var exercisesCompleted = 0
    val lines = mutableListOf<ExerciseSummaryLine>()

    detailStates.values.forEach { detail ->
        val sets = detail.completedSets
        totalSets += sets.size
        val target = detail.exercise.targetSets
        if (sets.isNotEmpty()) exercisesCompleted++

        var topWeight: Float? = null
        sets.forEach { record ->
            if (record is RepSetRecord) {
                totalVolume += record.reps * record.weightKg
                if (record.weightKg > maxWeight) maxWeight = record.weightKg
                if (topWeight == null || record.weightKg > topWeight!!) topWeight = record.weightKg
            }
        }

        lines.add(
            ExerciseSummaryLine(
                exerciseId = detail.exercise.id,
                name = detail.exercise.name,
                setsCompleted = sets.size,
                setsTarget = target,
                topWeight = topWeight
            )
        )
    }

    return SessionSummaryData(
        workoutName = workoutName,
        durationSecs = durationSecs,
        exercisesCompleted = exercisesCompleted,
        exercisesTotal = detailStates.size,
        totalSets = totalSets,
        totalVolumeKg = totalVolume,
        maxWeightKg = maxWeight,
        exerciseBreakdown = lines,
        tracks = tracks
    )
}

@Composable
fun SessionSummaryScreen(
    data: SessionSummaryData,
    onDeleteExercise: ((exerciseId: String) -> Unit)? = null,
    onGoHome: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ExerciseSummaryLine?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(64.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "אימון הושלם!",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        Text(
            data.workoutName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))

        // Stats grid — 2x2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "משך",
                value = formatDuration(data.durationSecs),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "תרגילים",
                value = "${data.exercisesCompleted}/${data.exercisesTotal}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "סטים",
                value = "${data.totalSets}",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "נפח כולל",
                value = if (data.totalVolumeKg > 0) "%,.0f ק״ג".format(data.totalVolumeKg) else "—",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Exercise breakdown
        Text(
            "פירוט תרגילים",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        data.exerciseBreakdown.forEach { line ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.shapes.small
                    )
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(
                        line.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    val detail = buildString {
                        append("${line.setsCompleted}/${line.setsTarget} סטים")
                        if (line.topWeight != null && line.topWeight > 0f) {
                            append(" · ${formatWeight(line.topWeight)} ק״ג")
                        }
                    }
                    Text(
                        detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (line.setsCompleted >= line.setsTarget && line.setsCompleted > 0) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (onDeleteExercise != null) {
                    IconButton(onClick = { deleteTarget = line }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "מחק תרגיל",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }

        if (data.tracks.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))

            Text(
                "♫ פסקול האימון",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            data.tracks.forEachIndexed { index, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            track.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                        if (track.artist.isNotBlank()) {
                            Text(
                                track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(
                "חזרה הביתה",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Spacer(Modifier.height(24.dp))
    }

    deleteTarget?.let { line ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("מחיקת תרגיל?") },
            text = { Text("למחוק את ${line.name} ואת כל הסטים שלו מהאימון?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteExercise?.invoke(line.exerciseId)
                    deleteTarget = null
                }) {
                    Text("מחק", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("ביטול")
                }
            }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = Purple
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

private fun formatWeight(w: Float): String =
    if (w == w.toLong().toFloat()) w.toLong().toString() else "%.1f".format(w)
