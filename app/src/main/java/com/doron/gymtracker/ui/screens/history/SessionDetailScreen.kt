package com.doron.gymtracker.ui.screens.history

import com.doron.gymtracker.data.templates.TemplateNames
import java.text.DateFormat
import com.doron.gymtracker.domain.model.nameIn
import com.doron.gymtracker.AppLocale
import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.data.ActiveClock
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.Laps
import com.doron.gymtracker.data.RouteDistance
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.TrainingTypes
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.Session
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.model.Weather
import com.doron.gymtracker.domain.usecase.GetPersonalRecordsUseCase
import com.doron.gymtracker.ui.components.builtinTypeName
import com.doron.gymtracker.ui.components.defaultNameRes
import com.doron.gymtracker.ui.screens.cardio.CardioUiState
import com.doron.gymtracker.ui.screens.cardio.RunSummaryScreen
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionDetailData(
    val sessionType: String = "GYM",
    val workoutName: String,
    /** Shown in place of [workoutName] for a run, a walk or a known workout. */
    @StringRes val workoutNameRes: Int? = null,
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
    val run: CardioUiState? = null,
    /** For a session of another type: what was entered with it. */
    val calories: Int? = null,
    val note: String? = null
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
    private val getPersonalRecords: GetPersonalRecordsUseCase,
    private val trainingTypes: TrainingTypes,
    private val templateNames: TemplateNames
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


    // Read on each use: the language can change while this view model lives
    private val dateFormat get() = DateFormat.getDateInstance(DateFormat.LONG, AppLocale.current())
    private val timeFormat get() = SimpleDateFormat("HH:mm", AppLocale.current())

    fun load(sessionId: Long) {
        viewModelScope.launch {
            val session = sessionRepo.getById(sessionId)
            if (session == null || session.finishedAt == null) {
                _isLoading.value = false
                return@launch
            }

            val isCardio = session.type != "GYM"
            val otherType = if (session.type == "OTHER") trainingTypes.get(session.activityTypeId) else null
            val workout = if (!isCardio) workoutRepo.getById(session.workoutId) else null
            val entries = if (!isCardio) sessionRepo.getEntriesForSession(session.id) else emptyList()

            var totalSets = 0
            var totalVolume = 0f
            var exercisesCompleted = 0
            val lines = mutableListOf<ExerciseBreakdownLine>()

            val allExercises = workout?.exercises ?: emptyList()

            entries.forEach { entry ->
                val exercise = allExercises.find { it.id == entry.exerciseId }
                val name = exercise?.nameIn(AppLocale.language()) ?: entry.exerciseId
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

            val durationSecs = session.durationS?.toLong() ?: ((session.finishedAt - session.startedAt) / 1000)
            val tracks = sessionRepo.getTracksForSession(session.id)

            val workoutNameRes = when (session.type) {
                "RUNNING" -> R.string.history_run
                "WALKING" -> R.string.history_walk
                "OTHER" -> otherType?.takeIf { it.name == null }?.let { builtinTypeName(it.id) }
                else -> workout?.takeIf { it.name == null }?.defaultNameRes()
            }
            val workoutName = otherType?.name ?: workout?.name?.let { templateNames.workout(it, AppLocale.language()) } ?: session.workoutId

            _state.value = SessionDetailData(
                sessionType = session.type,
                workoutName = workoutName,
                workoutNameRes = workoutNameRes,
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
                run = if (session.type == "RUNNING" || session.type == "WALKING") runReport(session, durationSecs, tracks) else null,
                calories = session.calories,
                note = session.note
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
            finishedAt = finishedAt,
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
            // No temperature saved: no weather, rather than a made-up 0°
            weather = details?.weatherTempC?.let { temp -> details.weatherCondition?.let { Weather(temp, 0, 0, it) } },
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

    // A run or walk has a page of its own, as when it ended
    data?.run?.let { run ->
        RunSummaryScreen(
            state = run,
            title = data?.let { d -> d.workoutNameRes?.let { stringResource(it) } ?: d.workoutName }.orEmpty(),
            onBack = onBack,
            savedRouteName = savedRouteName,
            onSaveAsRoute = viewModel::saveAsRoute
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(data?.let { d -> d.workoutNameRes?.let { stringResource(it) } ?: d.workoutName } ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.history_back))
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
                    StatCard(stringResource(R.string.history_duration), formatDuration(d.durationSecs), Modifier.weight(1f))
                    StatCard(stringResource(R.string.history_exercises), "${d.exercisesCompleted}/${d.exercisesTotal}", Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(stringResource(R.string.sets_label), "${d.totalSets}", Modifier.weight(1f))
                    StatCard(
                        stringResource(R.string.history_total_volume),
                        if (d.totalVolumeKg > 0) stringResource(R.string.weight_value, "%,.0f".format(d.totalVolumeKg)) else "—",
                        Modifier.weight(1f)
                    )
                }
            } else {
                // Another training type: how long, and what was entered with it
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(stringResource(R.string.history_duration), formatDuration(d.durationSecs), Modifier.weight(1f))
                    StatCard(stringResource(R.string.history_calories), d.calories?.toString() ?: "—", Modifier.weight(1f))
                }
                d.note?.let { note ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                            .padding(14.dp)
                    )
                }
            }

            if (d.sessionType == "GYM" && d.exerciseBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))

                Text(
                    stringResource(R.string.history_exercise_breakdown),
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
                            val detail = if (line.topWeight != null && line.topWeight > 0f) {
                                val w = if (line.topWeight == line.topWeight.toLong().toFloat())
                                    line.topWeight.toLong().toString() else "%.1f".format(line.topWeight)
                                stringResource(R.string.history_line_sets_weight, line.setsCompleted, line.setsTarget, w)
                            } else {
                                stringResource(R.string.history_line_sets, line.setsCompleted, line.setsTarget)
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
                    stringResource(R.string.history_soundtrack),
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
