package com.doron.gymtracker.ui.screens.cardio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.doron.gymtracker.ui.components.SOUNDTRACK_PREVIEW
import com.doron.gymtracker.ui.components.SoundtrackToggle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.R
import com.doron.gymtracker.data.Cadence
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.HeartRateSummary
import com.doron.gymtracker.data.Lap
import com.doron.gymtracker.data.Laps
import com.doron.gymtracker.data.Pace
import com.doron.gymtracker.ui.components.CadenceChart
import com.doron.gymtracker.ui.components.ElevationChart
import com.doron.gymtracker.ui.components.HeartRateChart
import com.doron.gymtracker.ui.components.PaceChart
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.components.appLocale
import com.doron.gymtracker.ui.theme.RunSummaryColors
import com.doron.gymtracker.ui.theme.RunSummaryShapes
import com.doron.gymtracker.ui.theme.RunSummaryType
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.doron.gymtracker.ui.components.distanceText
import com.doron.gymtracker.ui.components.distanceNumber
import com.doron.gymtracker.ui.components.distanceUnitLabel
import com.doron.gymtracker.ui.components.paceNumber
import com.doron.gymtracker.ui.components.paceText
import com.doron.gymtracker.ui.components.paceUnitLabel
import com.doron.gymtracker.ui.components.speedUnitLabel
import com.doron.gymtracker.ui.components.heightNumber
import com.doron.gymtracker.ui.components.heightText
import com.doron.gymtracker.ui.components.heightUnitLabel
import com.doron.gymtracker.ui.components.lapLengthText
import com.doron.gymtracker.ui.components.LocalDistanceUnit
import com.doron.gymtracker.data.speed

/**
 * A finished run or walk, top to bottom: where (the map), how far (the distance, then time and
 * pace), how it went (the details, pace and elevation, laps, songs), and what next (a bar fixed
 * at the bottom: save as a route, share). Shown when a run ends and again from History.
 */
@Composable
fun RunSummaryScreen(
    state: CardioUiState,
    title: String,
    onBack: () -> Unit,
    /** Name of the route this run was saved as, once it has been. */
    savedRouteName: String? = null,
    onSaveAsRoute: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }
    var mapExpanded by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }
    val hasRoute = state.route.size >= 2
    val heartRate = runHeartRate(state)

    Box(Modifier.fillMaxSize().background(RunSummaryColors.ground)) {
        Column(
            Modifier
                .fillMaxSize()
                // Outside the scroll, so the page never slides under the status bar
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                // The bar is drawn over the page: scroll its last card clear of it
                .padding(bottom = barHeight + RunSummaryShapes.sectionGap)
        ) {
            Header(title, subtitle(state.startedAt), onBack)
            Column(
                verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.sectionGap),
                modifier = Modifier
                    .padding(horizontal = RunSummaryShapes.sidePadding)
                    .padding(top = RunSummaryShapes.contentPaddingTop)
            ) {
                if (hasRoute) MapCard(state, onExpand = { mapExpanded = true })
                SummaryCard(state)
                DetailsGrid(details(state, (heartRate.value as? RunHeartRate.Shown)?.summary))
                if (heartRate.value == RunHeartRate.CanAsk) ConnectHeartRate(onConnect = heartRate.connect)
                ChartCards(state, (heartRate.value as? RunHeartRate.Shown)?.summary)
                if (state.laps.isNotEmpty()) LapsCard(state)
                if (state.tracks.isNotEmpty()) SoundtrackCard(state)
            }
        }
        ActionBar(
            saveLabel = savedRouteName?.let { stringResource(R.string.run_summary_saved_route, it) }
                ?: stringResource(R.string.run_summary_save_route),
            onSave = if (hasRoute) onSaveAsRoute else null,
            saved = savedRouteName != null,
            shareLabel = stringResource(if (state.type == "WALKING") R.string.run_summary_share_walk else R.string.run_summary_share_run),
            onShare = { sharing = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { barHeight = with(density) { it.height.toDp() } }
        )
    }

    ShareRunFlow(state, state.heldRecords, open = sharing, onClose = { sharing = false })
    if (mapExpanded) FullScreenMap(state, onClose = { mapExpanded = false })
}

/** "22 October 2026 · 09:27", in the app's language. */
@Composable
private fun subtitle(startedAt: Long?): String? {
    startedAt ?: return null
    val locale = appLocale()
    val date = DateFormat.getDateInstance(DateFormat.LONG, locale).format(Date(startedAt))
    val time = SimpleDateFormat("HH:mm", locale).format(Date(startedAt))
    return stringResource(R.string.run_summary_subtitle, date, time)
}

@Composable
private fun Header(title: String, subtitle: String?, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.headerGap),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = RunSummaryShapes.headerPaddingStart,
                end = RunSummaryShapes.headerPaddingEnd,
                top = RunSummaryShapes.headerPaddingTop,
                bottom = RunSummaryShapes.headerPaddingBottom
            )
    ) {
        val back = stringResource(R.string.run_summary_back)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(RunSummaryShapes.tapTarget)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = back, onClick = onBack)
                .semantics { contentDescription = back }
        ) {
            // Points to the start side: mirrored in right-to-left
            Icon(painterResource(R.drawable.ic_back), null, tint = RunSummaryColors.text, modifier = Modifier.size(RunSummaryShapes.backIcon))
        }
        Column(verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.labelValueGap), modifier = Modifier.weight(1f)) {
            Text(title, style = RunSummaryType.title, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = RunSummaryType.subtitle) }
        }
    }
}

@Composable
private fun MapCard(state: CardioUiState, onExpand: () -> Unit) {
    val description = stringResource(R.string.run_summary_map_description_distance, distanceText(state.distanceMeters))
    Box(
        Modifier
            .fillMaxWidth()
            .height(RunSummaryShapes.mapHeight)
            .clip(RoundedCornerShape(RunSummaryShapes.cardRadius))
            .background(RunSummaryColors.mapGround)
    ) {
        RunMap(
            modifier = Modifier.fillMaxSize().semantics { contentDescription = description },
            route = state.route,
            follow = false,
            routeColor = RunSummaryColors.accent,
            routeWidth = RunSummaryShapes.mapRouteWidth,
            markStart = true
        )
        state.weather?.let { weather ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.chipIconGap),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(RunSummaryShapes.mapOverlayInset)
                    .background(RunSummaryColors.surface, CircleShape)
                    .padding(horizontal = RunSummaryShapes.chipPaddingHorizontal, vertical = RunSummaryShapes.chipPaddingVertical)
            ) {
                Icon(painterResource(R.drawable.ic_cloud), null, tint = RunSummaryColors.textMuted, modifier = Modifier.size(RunSummaryShapes.chipIcon))
                Text(stringResource(R.string.run_summary_temp_at_start, weather.temperatureC), style = RunSummaryType.chip)
            }
        }
        MapButton(
            icon = R.drawable.ic_expand,
            label = stringResource(R.string.run_summary_expand_map),
            onClick = onExpand,
            modifier = Modifier.align(Alignment.TopEnd).padding(RunSummaryShapes.mapButtonInset)
        )
    }
}

/** A 32 dp white circle in a 44 dp tap target, over the map. */
@Composable
private fun MapButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(RunSummaryShapes.tapTarget)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(RunSummaryShapes.mapButton).background(RunSummaryColors.surface, CircleShape)
        ) {
            Icon(painterResource(icon), null, tint = RunSummaryColors.text, modifier = Modifier.size(RunSummaryShapes.mapButtonIcon))
        }
    }
}

/** The route to pan and zoom, over the whole screen. */
@Composable
private fun FullScreenMap(state: CardioUiState, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(RunSummaryColors.mapGround)) {
            RunMap(
                modifier = Modifier.fillMaxSize(),
                route = state.route,
                follow = false,
                interactive = true,
                routeColor = RunSummaryColors.accent,
                routeWidth = RunSummaryShapes.mapRouteWidth,
                markStart = true
            )
            MapButton(
                icon = R.drawable.ic_close,
                label = stringResource(R.string.run_summary_close_map),
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(RunSummaryShapes.mapButtonInset)
            )
        }
    }
}

/** The distance, large, then the time and the pace. */
@Composable
private fun SummaryCard(state: CardioUiState) {
    val section = stringResource(R.string.run_summary_section_summary)
    Column(
        verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.summaryGap),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = section }
            .background(RunSummaryColors.surface, RoundedCornerShape(RunSummaryShapes.cardRadius))
            .padding(
                start = RunSummaryShapes.summaryPaddingHorizontal,
                end = RunSummaryShapes.summaryPaddingHorizontal,
                top = RunSummaryShapes.summaryPaddingTop,
                bottom = RunSummaryShapes.summaryPaddingBottom
            )
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.labelValueGap)) {
            Text(stringResource(R.string.cardio_distance), style = RunSummaryType.label)
            // The figure, then its unit, in Hebrew too
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.distanceUnitGap)) {
                    Text(distanceNumber(state.distanceMeters), style = RunSummaryType.distance, modifier = Modifier.alignByBaseline())
                    Text(distanceUnitLabel(), style = RunSummaryType.distanceUnit, modifier = Modifier.alignByBaseline())
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.summaryColumnGap),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind { drawLine(RunSummaryColors.rule, Offset.Zero, Offset(size.width, 0f), RunSummaryShapes.divider.toPx()) }
                .padding(top = RunSummaryShapes.summaryGap)
        ) {
            LabelledValue(stringResource(R.string.cardio_duration), formatTime(state.elapsedSecs), null, Modifier.weight(1f))
            val pace = paceSecsPerKm(state)
            if (pace != null) {
                LabelledValue(
                    stringResource(R.string.cardio_avg_pace), paceNumber(pace), paceUnitLabel(),
                    Modifier.weight(1f)
                )
            } else Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun LabelledValue(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.labelValueGap), modifier = modifier) {
        Text(label, style = RunSummaryType.label)
        Text(valueWithUnit(value, unit, RunSummaryType.primaryUnit), style = RunSummaryType.primaryValue)
    }
}

/**
 * "7:54 /km": the unit smaller and muted, on the same baseline. Kept left to right in Hebrew too
 * (an isolate), so the figure always comes first.
 */
private fun valueWithUnit(value: String, unit: String?, unitStyle: TextStyle) = buildAnnotatedString {
    if (unit == null) {
        append(value)
        return@buildAnnotatedString
    }
    append(LTR_ISOLATE)
    append(value)
    append(" ")
    withStyle(SpanStyle(fontSize = unitStyle.fontSize, fontWeight = unitStyle.fontWeight, color = unitStyle.color)) { append(unit) }
    append(POP_ISOLATE)
}

private data class Detail(val value: String, val unit: String?, val label: String)

/** Only what this run recorded: a missing figure is left out, never shown as 0 or a dash. */
@Composable
private fun details(state: CardioUiState, heartRate: HeartRateSummary?): List<Detail> {
    val locale = appLocale()
    val hours = state.elapsedSecs / 3600.0
    return listOfNotNull(
        if (hours > 0 && state.distanceMeters > 0) {
            Detail("%.1f".format(locale, LocalDistanceUnit.current.speed(state.distanceMeters / 1000 / hours)), speedUnitLabel(), stringResource(R.string.cardio_avg_speed))
        } else null,
        state.avgCadence?.takeIf { it > 0 }?.let {
            Detail("$it", stringResource(R.string.run_summary_unit_spm), stringResource(R.string.run_summary_cadence))
        },
        heartRate?.let { Detail("${it.avgBpm}", stringResource(R.string.run_summary_unit_bpm), stringResource(R.string.run_summary_avg_heart_rate)) },
        heartRate?.let { Detail("${it.maxBpm}", stringResource(R.string.run_summary_unit_bpm), stringResource(R.string.run_summary_max_heart_rate)) },
        state.steps?.takeIf { it > 0 }?.let { Detail("%,d".format(locale, it), null, stringResource(R.string.cardio_steps)) },
        state.elevationGainM.takeIf { it.roundToInt() > 0 }?.let {
            Detail(heightNumber(it), heightUnitLabel(), stringResource(R.string.cardio_elevation_gain))
        },
        state.calories?.takeIf { it > 0 }?.let {
            Detail("$it", stringResource(R.string.run_summary_unit_kcal), stringResource(R.string.cardio_calories))
        },
        state.weather?.let {
            Detail(stringResource(R.string.run_summary_temp_value, it.temperatureC), null, stringResource(R.string.run_summary_temp_label))
        }
    )
}

/** Three across; the lines between cells are the 1 dp gaps over the divider colour. */
@Composable
private fun DetailsGrid(details: List<Detail>) {
    if (details.isEmpty()) return
    val section = stringResource(R.string.run_summary_section_details)
    Column(
        verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.divider),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = section }
            .clip(RoundedCornerShape(RunSummaryShapes.cardRadius))
            .background(RunSummaryColors.divider)
    ) {
        details.chunked(COLUMNS).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.divider),
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
            ) {
                row.forEach { DetailCell(it) }
                // A short last row: blank cells, so no divider colour shows where cells are missing
                repeat(COLUMNS - row.size) { Box(Modifier.weight(1f).fillMaxHeight().background(RunSummaryColors.surface)) }
            }
        }
    }
}

@Composable
private fun RowScope.DetailCell(detail: Detail) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.cellGap),
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .background(RunSummaryColors.surface)
            .padding(
                start = RunSummaryShapes.cellPaddingHorizontal,
                end = RunSummaryShapes.cellPaddingHorizontal,
                top = RunSummaryShapes.cellPaddingTop,
                bottom = RunSummaryShapes.cellPaddingBottom
            )
    ) {
        Text(valueWithUnit(detail.value, detail.unit, RunSummaryType.cellUnit), style = RunSummaryType.cellValue)
        Text(detail.label, style = RunSummaryType.cellLabel)
    }
}

/** A white card with a title (and a note at its end), as the design's cadence and laps cards. */
@Composable
private fun SectionCard(
    title: String,
    note: String? = null,
    horizontalPadding: Dp = RunSummaryShapes.sectionPadding,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.sectionContentGap),
        modifier = Modifier
            .fillMaxWidth()
            .background(RunSummaryColors.surface, RoundedCornerShape(RunSummaryShapes.cardRadius))
            .padding(top = RunSummaryShapes.sectionPadding, bottom = RunSummaryShapes.sectionPaddingBottom)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = RunSummaryShapes.sectionPadding)
        ) {
            Text(title, style = RunSummaryType.sectionTitle, modifier = Modifier.alignByBaseline().semantics { heading() })
            note?.let { Text(it, style = RunSummaryType.sectionNote, modifier = Modifier.alignByBaseline()) }
        }
        Box(Modifier.padding(horizontal = horizontalPadding)) { content() }
    }
}

@Composable
private fun ChartCards(state: CardioUiState, heartRate: HeartRateSummary?) {
    val pace = remember(state.route, state.pauses) {
        state.clock?.let { Pace.profile(state.route, it, state.pauses) }.orEmpty()
    }
    if (pace.size >= 2) {
        SectionCard(stringResource(R.string.cardio_pace)) { PaceChart(points = pace, modifier = Modifier.fillMaxWidth()) }
    }
    val profile = remember(state.route) { Elevation.profile(state.route, state.pauses) }
    if (profile.size >= 2) {
        SectionCard(stringResource(R.string.cardio_elevation)) { ElevationChart(points = profile, modifier = Modifier.fillMaxWidth()) }
    }
    if ((heartRate?.profile?.size ?: 0) >= 2) {
        SectionCard(stringResource(R.string.run_summary_heart_rate)) {
            HeartRateChart(points = heartRate!!.profile, modifier = Modifier.fillMaxWidth())
        }
    }
    val startedAt = state.startedAt
    val cadence = remember(state.stepMarks, state.moves, state.pauses, startedAt) {
        startedAt?.let { Cadence.profile(state.stepMarks, it, state.moves, state.pauses) }.orEmpty()
    }
    if (cadence.size >= 2) {
        val note = state.avgCadence?.takeIf { it > 0 }?.let { stringResource(R.string.run_summary_cadence_avg, it) }
        SectionCard(stringResource(R.string.run_summary_cadence), note = note) {
            CadenceChart(points = cadence, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** The run's heart rate from Health Connect, and how to ask for it. */
private class HeartRateHolder(val value: RunHeartRate, val connect: () -> Unit)

/**
 * Reads the run's heart rate when it's shown, and again on coming back: a watch often syncs
 * minutes after the run ends, or the user has just allowed it in Health Connect.
 */
@Composable
private fun runHeartRate(state: CardioUiState): HeartRateHolder {
    val viewModel = hiltViewModel<RunHeartRateViewModel>()
    val value by viewModel.state.collectAsState()
    val startedAt = state.startedAt
    val finishedAt = state.finishedAt
    LaunchedEffect(startedAt, finishedAt, state.moves) {
        if (startedAt != null && finishedAt != null) viewModel.load(startedAt, finishedAt, state.moves)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val launcher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
    }
    return HeartRateHolder(value) { launcher.launch(setOf(HealthConnectManager.READ_HEART_RATE)) }
}

/** A quiet line under the details: heart rate is there to have, if a watch records it. */
@Composable
private fun ConnectHeartRate(onConnect: () -> Unit) {
    val label = stringResource(R.string.run_summary_connect_heart_rate)
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RunSummaryShapes.tapTarget)
            .clip(RoundedCornerShape(RunSummaryShapes.buttonRadius))
            .clickable(role = Role.Button, onClick = onConnect)
            .padding(horizontal = RunSummaryShapes.cellPaddingHorizontal)
    ) {
        Text(label, style = RunSummaryType.sectionNote.copy(color = RunSummaryColors.secondaryButtonText))
    }
}

@Composable
private fun LapsCard(state: CardioUiState) {
    val laps = state.laps
    val climbs = remember(laps, state.route) { Laps.climbs(laps, state.route) }
    val cadences = remember(laps, state.stepMarks, state.startedAt) {
        state.startedAt?.let { Cadence.laps(laps, state.stepMarks, it) }?.takeIf { c -> c.any { it != null } }
    }
    // A short last stretch isn't a fair race against the full laps
    val fastest = laps.filterNot { it.partial }.takeIf { it.size > 1 }?.minByOrNull { it.paceSecsPerKm }
    val every = state.lapMeters.takeIf { it > 0 }?.let { stringResource(R.string.run_summary_laps_every_distance, lapLengthText(it)) }
    SectionCard(stringResource(R.string.cardio_laps), note = every, horizontalPadding = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            LapRow(
                height = null,
                cells = listOfNotNull(
                    stringResource(R.string.run_summary_lap), stringResource(R.string.run_summary_lap_time),
                    stringResource(R.string.run_summary_lap_pace_unit, paceUnitLabel()), climbs?.let { stringResource(R.string.run_summary_lap_climb) },
                    cadences?.let { stringResource(R.string.run_summary_cadence) }
                ).map { { Text(it, style = RunSummaryType.tableHeader) } }
            )
            laps.forEachIndexed { i, lap ->
                LapLine(lap, climbs?.get(i), cadence = cadences?.get(i), cadenceColumn = cadences != null, best = lap == fastest)
            }
        }
    }
}

@Composable
private fun LapLine(lap: Lap, climb: Double?, cadence: Int?, cadenceColumn: Boolean, best: Boolean) {
    val color = if (lap.partial) RunSummaryColors.textMuted else RunSummaryColors.text
    val cell = RunSummaryType.tableCell.copy(color = color)
    val fastestLabel = stringResource(R.string.run_summary_fastest_lap)
    val paceSpoken = paceText(lap.paceSecsPerKm)
    LapRow(
        height = if (lap.partial) RunSummaryShapes.partialLapRowHeight else RunSummaryShapes.lapRowHeight,
        background = if (best) RunSummaryColors.ground else null,
        ruled = true,
        cells = listOfNotNull(
            {
                Column {
                    Text("${lap.number}", style = cell.copy(fontWeight = FontWeight.Medium))
                    if (lap.partial) {
                        Text(distanceText(lap.distanceMeters), style = RunSummaryType.tableNote.copy(color = color))
                    }
                }
            },
            { Text(formatTime(lap.durationSecs), style = cell) },
            {
                if (best) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.fastestIconGap),
                        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "$fastestLabel, $paceSpoken" }
                    ) {
                        Icon(painterResource(R.drawable.ic_bolt), null, tint = RunSummaryColors.accent, modifier = Modifier.size(RunSummaryShapes.fastestIcon))
                        Text(paceNumber(lap.paceSecsPerKm), style = cell.copy(fontWeight = FontWeight.Bold, color = RunSummaryColors.accent))
                    }
                } else {
                    Text(paceNumber(lap.paceSecsPerKm), style = cell.copy(fontWeight = FontWeight.SemiBold))
                }
            },
            climb?.let { { Text(heightText(it), style = cell) } },
            // A lap without a believable figure leaves its cell empty
            if (cadenceColumn) ({ cadence?.let { Text("$it", style = cell) } }) else null
        )
    )
}

/** The lap number at the start, the figures lined up at the end; the first column a little narrower. */
@Composable
private fun LapRow(
    height: Dp?,
    cells: List<@Composable () -> Unit>,
    background: Color? = null,
    ruled: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (height != null) Modifier.height(height) else Modifier)
            .then(if (ruled) Modifier.drawBehind { drawLine(RunSummaryColors.rule, Offset.Zero, Offset(size.width, 0f), RunSummaryShapes.divider.toPx()) } else Modifier)
            .then(if (background != null) Modifier.background(background) else Modifier)
            .padding(horizontal = RunSummaryShapes.sectionPadding - RunSummaryShapes.lapCellPadding)
            .padding(vertical = if (height == null) 8.dp else 0.dp)
    ) {
        cells.forEachIndexed { i, cell ->
            Box(
                contentAlignment = if (i == 0) Alignment.CenterStart else Alignment.CenterEnd,
                modifier = Modifier.weight(if (i == 0) 0.8f else 1f).padding(horizontal = RunSummaryShapes.lapCellPadding)
            ) { cell() }
        }
    }
}

@Composable
private fun SoundtrackCard(state: CardioUiState) {
    SectionCard(stringResource(R.string.run_summary_soundtrack)) {
        var allTracks by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(RunSummaryShapes.trackRowGap)) {
            (if (allTracks) state.tracks else state.tracks.take(SOUNDTRACK_PREVIEW)).forEachIndexed { index, track ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", style = RunSummaryType.cellLabel, modifier = Modifier.width(24.dp))
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = RunSummaryType.tableCell.copy(fontWeight = FontWeight.Medium))
                        if (track.artist.isNotBlank()) Text(track.artist, style = RunSummaryType.cellLabel)
                    }
                }
            }
            SoundtrackToggle(
                total = state.tracks.size,
                expanded = allTracks,
                onToggle = { allTracks = !allTracks },
                color = RunSummaryColors.accent,
                style = RunSummaryType.tableCell
            )
        }
    }
}

/** Fixed to the bottom over the page, above the navigation bar: save as a route, and share. */
@Composable
private fun ActionBar(
    saveLabel: String,
    onSave: (() -> Unit)?,
    saved: Boolean,
    shareLabel: String,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.buttonGap),
        modifier = modifier
            .fillMaxWidth()
            .background(RunSummaryColors.surface)
            .drawBehind { drawLine(RunSummaryColors.divider, Offset.Zero, Offset(size.width, 0f), RunSummaryShapes.divider.toPx()) }
            .navigationBarsPadding()
            .padding(horizontal = RunSummaryShapes.barPaddingHorizontal, vertical = RunSummaryShapes.barPaddingVertical)
    ) {
        if (onSave != null) {
            BarButton(
                label = saveLabel,
                icon = R.drawable.ic_route,
                background = RunSummaryColors.secondaryButton,
                content = RunSummaryColors.secondaryButtonText,
                // Saved once: the button says so and stays put
                onClick = if (saved) null else onSave,
                modifier = Modifier.weight(RunSummaryShapes.secondaryWeight)
            )
        }
        BarButton(
            label = shareLabel,
            icon = R.drawable.ic_share,
            background = RunSummaryColors.accent,
            content = RunSummaryColors.surface,
            onClick = onShare,
            modifier = Modifier.weight(RunSummaryShapes.primaryWeight)
        )
    }
}

@Composable
private fun BarButton(
    label: String,
    @DrawableRes icon: Int,
    background: Color,
    content: Color,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(RunSummaryShapes.buttonRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RunSummaryShapes.buttonIconGap, Alignment.CenterHorizontally),
        modifier = modifier
            .height(RunSummaryShapes.buttonHeight)
            .clip(shape)
            .background(background)
            .clickable(enabled = onClick != null, role = Role.Button) { onClick?.invoke() }
            .padding(horizontal = 12.dp)
    ) {
        Icon(painterResource(icon), null, tint = content, modifier = Modifier.size(RunSummaryShapes.buttonIcon))
        Text(label, style = RunSummaryType.button.copy(color = content), maxLines = 1, textAlign = TextAlign.Center)
    }
}

private fun paceSecsPerKm(state: CardioUiState): Long? {
    if (state.elapsedSecs <= 0 || state.distanceMeters <= 0) return null
    val kmh = state.distanceMeters / 1000 / (state.elapsedSecs / 3600.0)
    // Slower than this is standing about, not a pace
    if (kmh <= 0.5) return null
    return (state.elapsedSecs * 1000 / state.distanceMeters).roundToLong()
}

private const val COLUMNS = 3
private const val LTR_ISOLATE = "\u2066"
private const val POP_ISOLATE = "\u2069"
