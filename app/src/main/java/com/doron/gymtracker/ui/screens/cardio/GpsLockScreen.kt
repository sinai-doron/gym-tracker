package com.doron.gymtracker.ui.screens.cardio

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.LocationTracker
import com.doron.gymtracker.data.RouteFollower
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.RouteRepository
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.domain.model.SavedRoute
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GpsLockState(
    val position: LocationPoint? = null,
    val isLocked: Boolean = false
) {
    val accuracy: Float? get() = position?.accuracy
}

@HiltViewModel
class GpsLockViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val sessionRepo: SessionRepository,
    private val settingsPrefs: SettingsPrefs,
    private val routeRepo: RouteRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GpsLockState())
    val state: StateFlow<GpsLockState> = _state.asStateFlow()

    // Starts with the last target used
    val target: StateFlow<RunTarget> = settingsPrefs.runTarget
        .stateIn(viewModelScope, SharingStarted.Eagerly, RunTarget())

    fun setTarget(target: RunTarget) {
        viewModelScope.launch {
            // Choosing "route" picks the newest route until another is chosen
            val withRoute = if (target.type == RunTargetType.ROUTE && target.routeId == null) {
                target.copy(routeId = routes.value.firstOrNull()?.id)
            } else target
            settingsPrefs.setRunTarget(withRoute)
        }
    }

    private val _routes = MutableStateFlow<List<SavedRoute>>(emptyList())
    /** Saved routes to choose from (without their points). */
    val routes: StateFlow<List<SavedRoute>> = _routes.asStateFlow()

    /** The route a route target follows, with its points, to show on the map. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedRoute: StateFlow<SavedRoute?> = target
        .mapLatest { t -> t.routeId?.takeIf { t.type == RunTargetType.ROUTE }?.let { routeRepo.get(it) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun loadRoutes() {
        viewModelScope.launch { _routes.value = routeRepo.getAll() }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        viewModelScope.launch {
            locationTracker.locationUpdates().collect { point ->
                val acc = point.accuracy
                _state.value = GpsLockState(
                    position = point,
                    isLocked = acc != null && acc < LOCK_ACCURACY_M
                )
            }
        }
    }

    suspend fun createSession(type: String): Long {
        return sessionRepo.startCardioSession(type)
    }

    private companion object {
        const val LOCK_ACCURACY_M = 20f
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLockScreen(
    type: String,
    viewModel: GpsLockViewModel,
    onReady: (sessionId: Long, type: String) -> Unit,
    onManageRoutes: () -> Unit,
    onBack: () -> Unit
) {
    val gpsState by viewModel.state.collectAsState()
    val target by viewModel.target.collectAsState()
    val routes by viewModel.routes.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    // Back from managing routes: there may be new ones
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.loadRoutes() }
    val needsRoute = target.type == RunTargetType.ROUTE && selectedRoute == null
    val scope = rememberCoroutineScope()
    val typeName = stringResource(if (type == "RUNNING") R.string.cardio_run else R.string.cardio_walk)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(typeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cardio_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The map, with a dot that settles as the fix gets precise; a pulse until the first fix
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Purple.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                val position = gpsState.position
                if (position == null) {
                    SearchingIndicator()
                } else {
                    RunMap(
                        modifier = Modifier.fillMaxSize(),
                        position = position,
                        plannedRoute = selectedRoute?.points ?: emptyList(),
                        dotColor = if (gpsState.isLocked) SuccessGreen else Purple,
                        // With a route, show all of it; otherwise follow the runner
                        follow = selectedRoute == null
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                stringResource(if (gpsState.isLocked) R.string.cardio_gps_ready else R.string.cardio_gps_searching),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (gpsState.isLocked) SuccessGreen else MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(6.dp))

            val accuracyText = gpsState.accuracy?.let { stringResource(R.string.cardio_gps_accuracy, it) }
                ?: stringResource(R.string.cardio_gps_waiting)
            Text(
                accuracyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!gpsState.isLocked && gpsState.accuracy != null) {
                Text(
                    stringResource(R.string.cardio_gps_go_outside),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            RunTargetPicker(
                target = target,
                routes = routes,
                selectedRoute = selectedRoute,
                onChange = viewModel::setTarget,
                onManageRoutes = onManageRoutes
            )
            val start = selectedRoute?.points?.firstOrNull()
            val here = gpsState.position
            if (start != null && here != null) {
                val away = RouteFollower.meters(start, RoutePoint(here.latitude, here.longitude))
                Text(
                    if (away < 50) stringResource(R.string.cardio_at_route_start)
                    else stringResource(R.string.cardio_route_starts_away, formatAway(away)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        val sessionId = viewModel.createSession(type)
                        onReady(sessionId, type)
                    }
                },
                enabled = gpsState.isLocked && !needsRoute,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text(
                    stringResource(if (type == "RUNNING") R.string.cardio_start_run else R.string.cardio_start_walk),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Dropdown for the kind of target, with a +/- stepper for its value next to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunTargetPicker(
    target: RunTarget,
    routes: List<SavedRoute>,
    selectedRoute: SavedRoute?,
    onChange: (RunTarget) -> Unit,
    onManageRoutes: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var choosingRoute by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = stringResource(targetTypeLabel(target.type)),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text(stringResource(R.string.cardio_target)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RunTargetType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(stringResource(targetTypeLabel(type))) },
                        onClick = {
                            onChange(target.copy(type = type))
                            expanded = false
                        }
                    )
                }
            }
        }
        when (target.type) {
            RunTargetType.DISTANCE -> ValueStepper(
                text = stringResource(R.string.km_value, formatKm(target.distanceKm)),
                onMinus = {
                    onChange(target.copy(distanceKm = (target.distanceKm - RunTarget.DISTANCE_STEP_KM).coerceAtLeast(RunTarget.DISTANCE_STEP_KM)))
                },
                onPlus = { onChange(target.copy(distanceKm = target.distanceKm + RunTarget.DISTANCE_STEP_KM)) }
            )
            RunTargetType.DURATION -> ValueStepper(
                text = stringResource(R.string.cardio_minutes_value, target.durationMin),
                onMinus = {
                    onChange(target.copy(durationMin = (target.durationMin - RunTarget.DURATION_STEP_MIN).coerceAtLeast(RunTarget.DURATION_STEP_MIN)))
                },
                onPlus = { onChange(target.copy(durationMin = target.durationMin + RunTarget.DURATION_STEP_MIN)) }
            )
            RunTargetType.ROUTE -> OutlinedButton(
                onClick = { choosingRoute = true },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text(
                    selectedRoute?.let { stringResource(R.string.cardio_route_name_km, it.name, it.distanceMeters / 1000) }
                        ?: stringResource(R.string.cardio_choose_route),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Purple
                )
            }
            RunTargetType.NONE -> Unit
        }
    }

    if (choosingRoute) {
        ModalBottomSheet(onDismissRequest = { choosingRoute = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(stringResource(R.string.cardio_choose_route), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                if (routes.isEmpty()) {
                    Text(
                        stringResource(R.string.cardio_no_routes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                routes.forEach { route ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onChange(target.copy(type = RunTargetType.ROUTE, routeId = route.id))
                                choosingRoute = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = route.id == selectedRoute?.id, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(route.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            stringResource(R.string.cardio_km_1dp, route.distanceMeters / 1000),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = { choosingRoute = false; onManageRoutes() }) {
                    Text(stringResource(R.string.cardio_manage_routes), color = Purple)
                }
            }
        }
    }
}

@Composable
private fun ValueStepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    // "− value +" in every language, like a number line
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
            Text(
                text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 72.dp)
            )
            FilledTonalIconButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun formatAway(meters: Double): String =
    if (meters < 1000) stringResource(R.string.meters_value, (meters / 10).toInt() * 10)
    else stringResource(R.string.cardio_km_1dp, meters / 1000)

@StringRes
private fun targetTypeLabel(type: RunTargetType): Int = when (type) {
    RunTargetType.NONE -> R.string.cardio_target_none
    RunTargetType.DISTANCE -> R.string.cardio_distance
    RunTargetType.DURATION -> R.string.cardio_time
    RunTargetType.ROUTE -> R.string.cardio_route
}

@Composable
private fun SearchingIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .alpha(alpha)
                .border(3.dp, Purple.copy(alpha = 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Purple.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("📡", fontSize = 40.sp)
        }
    }
}

@Composable
private fun LockedIndicator() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("✅", fontSize = 40.sp)
    }
}
