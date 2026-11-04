package com.doron.gymtracker.ui.screens.cardio

import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.doron.gymtracker.ui.components.reducedMotion
import com.doron.gymtracker.ui.theme.BeforeRunColors
import com.doron.gymtracker.ui.theme.BeforeRunShapes
import com.doron.gymtracker.ui.theme.BeforeRunType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
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

    /** Bars of signal, 1 to 4, for how precise the fix is; 1 before there is one. */
    val signalBars: Int get() = signalBars(accuracy)

    companion object {
        /** Precise enough to start: a fix to within this many metres. */
        const val LOCK_ACCURACY_M = 20f
        private const val STRONG_ACCURACY_M = 10f
        private const val FAIR_ACCURACY_M = 50f

        fun signalBars(accuracy: Float?): Int = when {
            accuracy == null -> 1
            accuracy <= STRONG_ACCURACY_M -> 4
            accuracy < LOCK_ACCURACY_M -> 3
            accuracy < FAIR_ACCURACY_M -> 2
            else -> 1
        }
    }
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
                    isLocked = acc != null && acc < GpsLockState.LOCK_ACCURACY_M
                )
            }
        }
    }

    suspend fun createSession(type: String): Long {
        return sessionRepo.startCardioSession(type)
    }
}

/** What the start button can do: wait for GPS, wait for a route to follow, or start. */
private enum class StartState { WAITING_FOR_GPS, NEEDS_ROUTE, READY }

/**
 * Before a run or walk: the map over the whole screen, and one panel at the bottom with where
 * GPS is up to, the target, and the start button. Until the fix is precise enough the map is
 * veiled, rings pulse where you are expected, and the button says what it is waiting for.
 */
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
    val locked = gpsState.isLocked
    var choosingTarget by remember { mutableStateOf(false) }
    var recenter by remember { mutableIntStateOf(0) }
    // The map shows above the panel: the dot, the rings and the map's buttons stay there
    var panelHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize().background(BeforeRunColors.mapGround)) {
        gpsState.position?.let { position ->
            val description = position.accuracy?.let { stringResource(R.string.before_run_map_description, accuracyText(it)) }
            RunMap(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { description?.let { contentDescription = it } },
                position = position,
                plannedRoute = selectedRoute?.points ?: emptyList(),
                dotColor = BeforeRunColors.accent,
                // With a route, show all of it; otherwise follow the runner
                follow = selectedRoute == null,
                interactive = true,
                bottomInset = panelHeight,
                recenter = recenter
            )
        }
        // Veiled until GPS is ready, then the map comes up in full colour
        val veil by animateFloatAsState(if (locked) 0f else BeforeRunShapes.veilAlpha, tween(FADE_MS), label = "veil")
        Box(Modifier.fillMaxSize().drawBehind { drawRect(BeforeRunColors.softSurface, alpha = veil) })

        Box(Modifier.fillMaxSize().padding(bottom = panelHeight)) {
            AnimatedVisibility(!locked, enter = fadeIn(tween(FADE_MS)), exit = fadeOut(tween(FADE_MS))) {
                SearchingRings()
            }
            AnimatedVisibility(
                locked,
                enter = fadeIn(tween(FADE_MS)),
                exit = fadeOut(tween(FADE_MS)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = BeforeRunShapes.sideInset, bottom = BeforeRunShapes.mapButtonGap)
            ) {
                RoundMapButton(R.drawable.ic_locate, BeforeRunShapes.mapButtonIcon, stringResource(R.string.before_run_recentre), onClick = { recenter++ })
            }
        }

        // Points to the start side: mirrored in right-to-left
        RoundMapButton(
            R.drawable.ic_back,
            BeforeRunShapes.backIcon,
            stringResource(R.string.cardio_back),
            onBack,
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = BeforeRunShapes.sideInset, top = BeforeRunShapes.backTop)
        )

        val panelShape = RoundedCornerShape(topStart = BeforeRunShapes.panelRadius, topEnd = BeforeRunShapes.panelRadius)
        Column(
            verticalArrangement = Arrangement.spacedBy(BeforeRunShapes.panelGap),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { panelHeight = with(density) { it.height.toDp() } }
                .shadow(BeforeRunShapes.panelShadow, panelShape, ambientColor = BeforeRunColors.shadow, spotColor = BeforeRunColors.shadow)
                .background(BeforeRunColors.panel, panelShape)
                // Touches on the panel stay on the panel, not the map under it
                .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
                .padding(start = BeforeRunShapes.sideInset, end = BeforeRunShapes.sideInset, top = BeforeRunShapes.panelPaddingTop)
                .navigationBarsPadding()
                .padding(bottom = BeforeRunShapes.panelPaddingBottom)
        ) {
            StatusRow(gpsState)
            TargetRow(
                value = targetValue(target, selectedRoute),
                note = routeStartNote(selectedRoute, gpsState.position),
                onClick = { choosingTarget = true }
            )
            StartButton(
                state = when {
                    !locked -> StartState.WAITING_FOR_GPS
                    needsRoute -> StartState.NEEDS_ROUTE
                    else -> StartState.READY
                },
                startLabel = stringResource(if (type == "RUNNING") R.string.cardio_start_run else R.string.cardio_start_walk),
                onStart = {
                    scope.launch {
                        val sessionId = viewModel.createSession(type)
                        onReady(sessionId, type)
                    }
                }
            )
        }
    }

    if (choosingTarget) {
        TargetSheet(
            target = target,
            routes = routes,
            selectedRoute = selectedRoute,
            onChange = viewModel::setTarget,
            onManageRoutes = {
                choosingTarget = false
                onManageRoutes()
            },
            onDismiss = { choosingTarget = false }
        )
    }
}

/** A white circle over the map: back, and centre on me. */
@Composable
private fun RoundMapButton(@DrawableRes icon: Int, iconSize: Dp, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(BeforeRunShapes.tapTarget)
            .shadow(BeforeRunShapes.buttonShadow, CircleShape, ambientColor = BeforeRunColors.shadow, spotColor = BeforeRunColors.shadow)
            .background(BeforeRunColors.panel, CircleShape)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Icon(painterResource(icon), null, tint = BeforeRunColors.text, modifier = Modifier.size(iconSize))
    }
}

/** Three rings pulsing out from a pin where you are expected; still when animations are off. */
@Composable
private fun SearchingRings() {
    val phase = loop(PULSE_MS, "rings")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val step = BeforeRunShapes.ringStep.toPx()
            // Each beat every ring moves out a step and takes on the look of the ring it replaces;
            // a new one grows from the middle and the outermost fades away
            val alphas = listOf(0f) + BeforeRunShapes.ringAlphas + 0f
            val strokes = BeforeRunShapes.ringStrokes.map { it.toPx() }.let { listOf(it.first()) + it + it.last() }
            for (ring in 0 until alphas.size - 1) {
                val at = ring + phase.value
                drawCircle(
                    BeforeRunColors.accent,
                    radius = step * at,
                    alpha = along(alphas, at),
                    style = Stroke(along(strokes, at))
                )
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(BeforeRunShapes.pinCircle).background(BeforeRunColors.panel, CircleShape)
        ) {
            Icon(painterResource(R.drawable.ic_location_pin), null, tint = BeforeRunColors.accent, modifier = Modifier.size(BeforeRunShapes.pinIcon))
        }
    }
}

/** [values] at a point [at] between their indexes. */
private fun along(values: List<Float>, at: Float): Float {
    val i = at.toInt().coerceIn(0, values.size - 2)
    return values[i] + (values[i + 1] - values[i]) * (at - i)
}

/** 0 to 1 over and over, every [periodMs]; held at 0 when animations are off. */
@Composable
private fun loop(periodMs: Int, label: String): State<Float> {
    if (reducedMotion()) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label).animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodMs, easing = LinearEasing)),
        label = label
    )
}

/** Where GPS is up to; read out when it changes from finding to ready. */
@Composable
private fun StatusRow(state: GpsLockState) {
    val locked = state.isLocked
    val signal = stringResource(signalLabel(state.signalBars))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BeforeRunShapes.statusGap),
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedContent(
            targetState = locked,
            transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) using SizeTransform(clip = false) },
            contentAlignment = Alignment.CenterStart,
            label = "status",
            modifier = Modifier.weight(1f)
        ) { ready ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BeforeRunShapes.statusGap)
            ) {
                StatusIcon(ready)
                Column(verticalArrangement = Arrangement.spacedBy(BeforeRunShapes.titleGap)) {
                    Text(
                        stringResource(if (ready) R.string.before_run_ready_title else R.string.before_run_finding_title),
                        style = BeforeRunType.statusTitle,
                        // Only the title is live: the accuracy changes every second
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    Text(
                        state.accuracy?.takeIf { ready }?.let { stringResource(R.string.before_run_accuracy, accuracyText(it)) }
                            ?: stringResource(R.string.before_run_finding_hint),
                        style = BeforeRunType.statusSubtitle
                    )
                }
            }
        }
        SignalBars(
            bars = state.signalBars,
            filled = if (locked) BeforeRunColors.success else BeforeRunColors.accent,
            modifier = Modifier.semantics { contentDescription = signal }
        )
    }
}

@Composable
private fun StatusIcon(ready: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BeforeRunShapes.statusIcon)
            .background(if (ready) BeforeRunColors.successTint else BeforeRunColors.accentTint, CircleShape)
    ) {
        if (ready) {
            Icon(painterResource(R.drawable.ic_check), null, tint = BeforeRunColors.success, modifier = Modifier.size(BeforeRunShapes.checkIcon))
        } else {
            Spinner(
                color = BeforeRunColors.accent,
                track = BeforeRunColors.track,
                size = BeforeRunShapes.spinner,
                stroke = BeforeRunShapes.spinnerStroke,
                radius = STATUS_SPINNER_RADIUS,
                sweep = STATUS_SPINNER_SWEEP
            )
        }
    }
}

/** An arc going round, as the design draws it: [radius] is a share of [size], [sweep] in degrees. */
@Composable
private fun Spinner(color: Color, size: Dp, stroke: Dp, radius: Float, sweep: Float, track: Color? = null) {
    val turn = loop(SPIN_MS, "spinner")
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension * radius
        val width = stroke.toPx()
        track?.let { drawCircle(it, r, style = Stroke(width)) }
        rotate(turn.value * 360f) {
            drawArc(
                color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = center - Offset(r, r),
                size = Size(r * 2, r * 2),
                style = Stroke(width, cap = StrokeCap.Round)
            )
        }
    }
}

/** Four bars growing from the start side, [bars] of them filled. */
@Composable
private fun SignalBars(bars: Int, filled: Color, modifier: Modifier = Modifier) {
    val color by animateColorAsState(filled, tween(FADE_MS), label = "bars")
    val heights = BeforeRunShapes.barHeights
    val width = BeforeRunShapes.barWidth * heights.size + BeforeRunShapes.barGap * (heights.size - 1)
    Canvas(modifier.size(width, heights.last())) {
        val barWidth = BeforeRunShapes.barWidth.toPx()
        val step = barWidth + BeforeRunShapes.barGap.toPx()
        val corner = CornerRadius(BeforeRunShapes.barRadius.toPx())
        heights.forEachIndexed { i, height ->
            val x = step * i
            val h = height.toPx()
            drawRoundRect(
                if (i < bars) color else BeforeRunColors.track,
                topLeft = Offset(if (layoutDirection == LayoutDirection.Rtl) size.width - x - barWidth else x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = corner
            )
        }
    }
}

@StringRes
private fun signalLabel(bars: Int): Int = when (bars) {
    4 -> R.string.before_run_signal_4
    3 -> R.string.before_run_signal_3
    2 -> R.string.before_run_signal_2
    else -> R.string.before_run_signal_1
}

/** "±5", left to right in every language; the unit goes with the words around it. */
private fun accuracyText(meters: Float): String = LTR_ISOLATE + "±" + meters.roundToInt() + POP_ISOLATE

/** The target, in a row that opens the picker. */
@Composable
private fun TargetRow(value: String, note: String?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BeforeRunShapes.statusGap),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BeforeRunShapes.targetMinHeight)
            .clip(RoundedCornerShape(BeforeRunShapes.buttonRadius))
            .background(BeforeRunColors.softSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = BeforeRunShapes.sideInset, vertical = BeforeRunShapes.targetPaddingVertical)
    ) {
        Icon(painterResource(R.drawable.ic_target), null, tint = BeforeRunColors.textMuted, modifier = Modifier.size(BeforeRunShapes.targetIcon))
        Column(verticalArrangement = Arrangement.spacedBy(BeforeRunShapes.targetLineGap), modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.cardio_target), style = BeforeRunType.targetLabel)
            Text(value, style = BeforeRunType.targetValue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            note?.let { Text(it, style = BeforeRunType.targetNote) }
        }
        // Points to the end side: mirrored in right-to-left
        Icon(painterResource(R.drawable.ic_chevron), null, tint = BeforeRunColors.textMuted, modifier = Modifier.size(BeforeRunShapes.chevron))
    }
}

@Composable
private fun targetValue(target: RunTarget, route: SavedRoute?): String = when (target.type) {
    RunTargetType.NONE -> stringResource(R.string.cardio_target_none)
    RunTargetType.DISTANCE -> stringResource(
        R.string.before_run_target_value,
        stringResource(R.string.cardio_distance),
        stringResource(R.string.km_value, formatKm(target.distanceKm))
    )
    RunTargetType.DURATION -> stringResource(
        R.string.before_run_target_value,
        stringResource(R.string.cardio_time),
        stringResource(R.string.cardio_minutes_value, target.durationMin)
    )
    RunTargetType.ROUTE -> route?.let { stringResource(R.string.cardio_route_name_km, it.name, it.distanceMeters / 1000) }
        ?: stringResource(R.string.cardio_choose_route)
}

/** How far the route to follow starts from here, once there is a fix. */
@Composable
private fun routeStartNote(route: SavedRoute?, here: LocationPoint?): String? {
    val start = route?.points?.firstOrNull() ?: return null
    if (here == null) return null
    val away = RouteFollower.meters(start, RoutePoint(here.latitude, here.longitude))
    return if (away < 50) stringResource(R.string.cardio_at_route_start)
    else stringResource(R.string.cardio_route_starts_away, formatAway(away))
}

/** Start, or what it waits for; it changes colour rather than fading, so it reads either way. */
@Composable
private fun StartButton(state: StartState, startLabel: String, onStart: () -> Unit) {
    val enabled = state == StartState.READY
    val background by animateColorAsState(if (enabled) BeforeRunColors.accent else BeforeRunColors.disabledButton, tween(FADE_MS), label = "button")
    val content by animateColorAsState(if (enabled) BeforeRunColors.panel else BeforeRunColors.disabledButtonText, tween(FADE_MS), label = "buttonText")
    val reason = when (state) {
        StartState.WAITING_FOR_GPS -> stringResource(R.string.before_run_waiting_reason)
        StartState.NEEDS_ROUTE -> stringResource(R.string.before_run_needs_route_reason)
        StartState.READY -> null
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(BeforeRunShapes.buttonHeight)
            .clip(RoundedCornerShape(BeforeRunShapes.buttonRadius))
            .background(background)
            .clickable(enabled = enabled, role = Role.Button, onClick = onStart)
            // Announced as unavailable, and why
            .semantics { reason?.let { stateDescription = it } }
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(FADE_MS)) togetherWith fadeOut(tween(FADE_MS)) },
            label = "buttonLabel"
        ) { shown ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BeforeRunShapes.buttonIconGap)
            ) {
                when (shown) {
                    StartState.WAITING_FOR_GPS -> Spinner(
                        color = content,
                        size = BeforeRunShapes.buttonIcon,
                        stroke = BeforeRunShapes.buttonSpinnerStroke,
                        radius = BUTTON_SPINNER_RADIUS,
                        sweep = BUTTON_SPINNER_SWEEP
                    )
                    StartState.NEEDS_ROUTE ->
                        Icon(painterResource(R.drawable.ic_route), null, tint = content, modifier = Modifier.size(BeforeRunShapes.buttonIcon))
                    StartState.READY ->
                        Icon(painterResource(R.drawable.ic_play), null, tint = content, modifier = Modifier.size(BeforeRunShapes.buttonIcon))
                }
                Text(
                    when (shown) {
                        StartState.WAITING_FOR_GPS -> stringResource(R.string.before_run_waiting)
                        StartState.NEEDS_ROUTE -> stringResource(R.string.cardio_choose_route)
                        StartState.READY -> startLabel
                    },
                    style = BeforeRunType.button.copy(color = content),
                    maxLines = 1
                )
            }
        }
    }
}

/** The kind of target, its distance or time, or the route to follow. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetSheet(
    target: RunTarget,
    routes: List<SavedRoute>,
    selectedRoute: SavedRoute?,
    onChange: (RunTarget) -> Unit,
    onManageRoutes: () -> Unit,
    onDismiss: () -> Unit
) {
    val radioColors = RadioButtonDefaults.colors(selectedColor = BeforeRunColors.accent)
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BeforeRunColors.panel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BeforeRunShapes.sideInset)
                .padding(bottom = BeforeRunShapes.panelGap)
        ) {
            Text(
                stringResource(R.string.cardio_target),
                style = BeforeRunType.sheetTitle,
                modifier = Modifier.padding(bottom = 8.dp).semantics { heading() }
            )
            Column(Modifier.selectableGroup()) {
                RunTargetType.entries.forEach { type ->
                    val selected = target.type == type
                    SheetOption(
                        label = stringResource(targetTypeLabel(type)),
                        selected = selected,
                        radioColors = radioColors,
                        onClick = { onChange(target.copy(type = type)) }
                    ) {
                        if (selected) when (type) {
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
                            RunTargetType.ROUTE, RunTargetType.NONE -> Unit
                        }
                    }
                }
            }

            if (target.type == RunTargetType.ROUTE) {
                Text(
                    stringResource(R.string.cardio_choose_route),
                    style = BeforeRunType.statusTitle,
                    modifier = Modifier.padding(top = BeforeRunShapes.panelGap, bottom = 4.dp).semantics { heading() }
                )
                if (routes.isEmpty()) {
                    Text(stringResource(R.string.cardio_no_routes), style = BeforeRunType.statusSubtitle)
                }
                Column(Modifier.selectableGroup()) {
                    routes.forEach { route ->
                        SheetOption(
                            label = route.name,
                            selected = route.id == selectedRoute?.id,
                            radioColors = radioColors,
                            onClick = { onChange(target.copy(type = RunTargetType.ROUTE, routeId = route.id)) }
                        ) {
                            Text(stringResource(R.string.cardio_km_1dp, route.distanceMeters / 1000), style = BeforeRunType.statusSubtitle)
                        }
                    }
                }
                TextButton(onClick = onManageRoutes) {
                    Text(stringResource(R.string.cardio_manage_routes), color = BeforeRunColors.accent)
                }
            }

            Spacer(Modifier.height(BeforeRunShapes.panelGap))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BeforeRunShapes.buttonHeight)
                    .clip(RoundedCornerShape(BeforeRunShapes.buttonRadius))
                    .background(BeforeRunColors.accent)
                    .clickable(role = Role.Button, onClick = onDismiss)
            ) {
                Text(stringResource(R.string.before_run_target_done), style = BeforeRunType.button.copy(color = BeforeRunColors.panel))
            }
        }
    }
}

/** A radio row in the target sheet, with room at the end for its value. */
@Composable
private fun SheetOption(
    label: String,
    selected: Boolean,
    radioColors: RadioButtonColors,
    onClick: () -> Unit,
    end: @Composable () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BeforeRunShapes.sheetOptionHeight)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        RadioButton(selected = selected, onClick = null, colors = radioColors)
        Spacer(Modifier.width(BeforeRunShapes.statusGap))
        Text(label, style = BeforeRunType.option, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        end()
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

private const val FADE_MS = 300
private const val PULSE_MS = 2400
private const val SPIN_MS = 1000
// The design's arcs: a quarter turn at 8 of 24 in the status circle, three quarters at 9 of 24 in the button
private const val STATUS_SPINNER_RADIUS = 8f / 24
private const val STATUS_SPINNER_SWEEP = 90f
private const val BUTTON_SPINNER_RADIUS = 9f / 24
private const val BUTTON_SPINNER_SWEEP = 270f
private const val LTR_ISOLATE = "\u2066"
private const val POP_ISOLATE = "\u2069"
