package com.doron.gymtracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SessionDetailData(
    val sessionType: String = "GYM",
    val workoutName: String,
    val dateLabel: String,
    val timeRange: String,
    val durationSecs: Long,
    val exercisesCompleted: Int,
    val exercisesTotal: Int,
    val totalSets: Int,
    val totalVolumeKg: Float,
    val distanceKm: Double? = null,
    val exerciseBreakdown: List<ExerciseBreakdownLine>,
    val tracks: List<TrackLog> = emptyList()
)

data class ExerciseBreakdownLine(
    val exerciseId: String,
    val name: String,
    val setsCompleted: Int,
    val setsTarget: Int,
    val topWeight: Float?
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow<SessionDetailData?>(null)
    val state: StateFlow<SessionDetailData?> = _state.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private val heLocale = Locale("he")
    private val dateFormat = SimpleDateFormat("dd בMMMM yyyy", heLocale)
    private val timeFormat = SimpleDateFormat("HH:mm", heLocale)

    fun load(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepo.getById(sessionId)
            if (session == null || session.finishedAt == null) {
                _isLoading.value = false
                return@launch
            }

            val isCardio = session.type != "GYM"
            val workout = if (!isCardio) workoutRepo.getById(session.workoutId) else null
            val entries = if (!isCardio) sessionRepo.getEntriesForSession(session.id) else emptyList()

            var totalSets = 0
            var totalVolume = 0f
            var exercisesCompleted = 0
            val lines = mutableListOf<ExerciseBreakdownLine>()

            val allExercises = workout?.exercises ?: emptyList()

            entries.forEach { entry ->
                val exercise = allExercises.find { it.id == entry.exerciseId }
                val name = exercise?.name ?: entry.exerciseId
                val target = exercise?.targetSets ?: entry.sets.size
                val sets = entry.sets
                totalSets += sets.size
                if (sets.isNotEmpty()) exercisesCompleted++

                var topWeight: Float? = null
                sets.forEach { set ->
                    if (set is RepSetRecord) {
                        totalVolume += set.reps * set.weightKg
                        if (topWeight == null || set.weightKg > topWeight!!) topWeight = set.weightKg
                    }
                }
                lines.add(ExerciseBreakdownLine(entry.exerciseId, name, sets.size, target, topWeight))
            }

            val durationSecs = (session.finishedAt - session.startedAt) / 1000
            val tracks = sessionRepo.getTracksForSession(session.id)

            val workoutName = when (session.type) {
                "RUNNING" -> "ריצה"
                "WALKING" -> "הליכה"
                else -> workout?.label?.displayName ?: session.workoutId
            }

            _state.value = SessionDetailData(
                sessionType = session.type,
                workoutName = workoutName,
                dateLabel = dateFormat.format(Date(session.startedAt)),
                timeRange = "${timeFormat.format(Date(session.startedAt))} – ${timeFormat.format(Date(session.finishedAt))}",
                durationSecs = durationSecs,
                exercisesCompleted = exercisesCompleted,
                exercisesTotal = allExercises.size,
                totalSets = totalSets,
                totalVolumeKg = totalVolume,
                distanceKm = session.distanceMeters?.let { it / 1000.0 },
                exerciseBreakdown = lines,
                tracks = tracks
            )
            _isLoading.value = false
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit
) {
    val data by viewModel.state.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.workoutName ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val d = data ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                d.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                d.timeRange,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            if (d.sessionType == "GYM") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("משך", formatDuration(d.durationSecs), Modifier.weight(1f))
                    StatCard("תרגילים", "${d.exercisesCompleted}/${d.exercisesTotal}", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("סטים", "${d.totalSets}", Modifier.weight(1f))
                    StatCard(
                        "נפח כולל",
                        if (d.totalVolumeKg > 0) "%,.0f ק״ג".format(d.totalVolumeKg) else "—",
                        Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("משך", formatDuration(d.durationSecs), Modifier.weight(1f))
                    StatCard("מרחק", d.distanceKm?.let { "%.2f ק״מ".format(it) } ?: "—", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                val avgSpeedKmh = if (d.durationSecs > 0 && d.distanceKm != null) {
                    d.distanceKm / (d.durationSecs / 3600.0)
                } else 0.0
                val avgPace = if (avgSpeedKmh > 0.5) {
                    val paceMin = (60.0 / avgSpeedKmh).toInt()
                    val paceSec = ((60.0 / avgSpeedKmh - paceMin) * 60).toInt()
                    "%d:%02d /ק״מ".format(paceMin, paceSec)
                } else "—"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard("מהירות ממוצעת", "%.1f קמ״ש".format(avgSpeedKmh), Modifier.weight(1f))
                    StatCard("קצב ממוצע", avgPace, Modifier.weight(1f))
                }
            }

            if (d.sessionType == "GYM" && d.exerciseBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))

                Text(
                    "פירוט תרגילים",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                d.exerciseBreakdown.forEach { line ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                line.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            val detail = buildString {
                                append("${line.setsCompleted}/${line.setsTarget} סטים")
                                if (line.topWeight != null && line.topWeight > 0f) {
                                    val w = if (line.topWeight == line.topWeight.toLong().toFloat())
                                        line.topWeight.toLong().toString() else "%.1f".format(line.topWeight)
                                    append(" · $w ק״ג")
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
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            if (d.tracks.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))

                Text(
                    "♫ פסקול האימון",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                d.tracks.forEachIndexed { index, track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
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
        }
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
