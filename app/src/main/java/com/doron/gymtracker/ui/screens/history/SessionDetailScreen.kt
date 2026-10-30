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
import com.doron.gymtracker.data.ActiveClock
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.Laps
import com.doron.gymtracker.data.RouteDistance
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.usecase.GetPersonalRecordsUseCase
import com.doron.gymtracker.ui.screens.cardio.CardioUiState
import com.doron.gymtracker.ui.screens.cardio.RunReport
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val tracks: List<TrackLog> = emptyList(),
    /** For a run or walk: everything recorded, shown like the finish screen. */
    val run: CardioUiState? = null
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
    private val workoutRepo: WorkoutRepository,
    private val routeRepo: RouteRepository,
    private val getPersonalRecords: GetPersonalRecordsUseCase
) : ViewModel() {

    private val _savedRouteName = MutableStateFlow<String?>(null)
    /** The name this run was saved under as a route, once saved. */
    val savedRouteName: StateFlow<String?> = _savedRouteName.asStateFlow()

    /** Keeps this run's route to run again; it's named after where it starts. */
    fun saveAsRoute() {
        val route = _state.value?.run?.route ?: return
        viewModelScope.launch {
            val id = routeRepo.save(route.map { RoutePoint(it.latitude, it.longitude, it.altitude) })
            _savedRouteName.value = routeRepo.get(id)?.name
        }
    }


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
                tracks = tracks,
                run = if (isCardio) runReport(session, durationSecs, tracks) else null
            )
            _isLoading.value = false
        }
    }

    /** A finished run as the finish screen shows it, rebuilt from what was saved. */
    private suspend fun runReport(session: Session, durationSecs: Long, tracks: List<TrackLog>): CardioUiState {
        val route = sessionRepo.getRouteForSession(session.id)
        // Runs from before run details were kept have none: work out what the route allows
        val details = sessionRepo.getRunDetails(session.id)
        val lapMeters = details?.lapMeters ?: 0
        // Moving time, as during the run: auto-paused stretches don't count
        val pauses = sessionRepo.getPauses(session.id)
        val clock = ActiveClock.forRoute(session.startedAt, route, pauses)
        val finishedAt = session.finishedAt ?: (session.startedAt + durationSecs * 1000)
        return CardioUiState(
            type = session.type,
            startedAt = session.startedAt,
            elapsedSecs = clock.activeMs(finishedAt) / 1000,
            distanceMeters = session.distanceMeters ?: RouteDistance.total(route, pauses),
            route = route,
            tracks = tracks,
            target = details?.target ?: RunTarget(),
            lapMeters = lapMeters,
            laps = Laps.split(session.startedAt, route, lapMeters.toDouble(), clock, finishedAt, pauses),
            pauses = pauses,
            // What the report's clock (and so the pace chart) is worked out from
            moves = RouteDistance.marks(route, pauses),
            steps = details?.steps,
            elevationGainM = details?.elevationGainM ?: Elevation.gain(route),
            weather = details?.weatherCondition?.let { Weather(details.weatherTempC ?: 0, 0, 0, it) },
            calories = details?.calories,
            heldRecords = if (session.type == "RUNNING") getPersonalRecords().filter { it.sessionId == session.id } else emptyList(),
            isFinished = true
        )
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
    val savedRouteName by viewModel.savedRouteName.collectAsState()

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
            } else if (d.run != null) {
                RunReport(d.run, savedRouteName = savedRouteName, onSaveAsRoute = viewModel::saveAsRoute)
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

            // A run lists its songs in its report
            if (d.tracks.isNotEmpty() && d.run == null) {
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
