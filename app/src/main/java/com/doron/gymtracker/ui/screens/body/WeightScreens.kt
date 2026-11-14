package com.doron.gymtracker.ui.screens.body

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.data.of
import com.doron.gymtracker.data.prefs.WeightUnit
import com.doron.gymtracker.data.toKg
import com.doron.gymtracker.domain.model.WeighIn
import com.doron.gymtracker.domain.model.WeightGoal
import com.doron.gymtracker.domain.model.WeightStats
import com.doron.gymtracker.ui.components.DesignButton
import com.doron.gymtracker.ui.components.DesignDialog
import com.doron.gymtracker.ui.components.LocalWeightUnit
import com.doron.gymtracker.ui.components.appLocale
import com.doron.gymtracker.ui.components.weightUnitLabel
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppType
import com.doron.gymtracker.ui.theme.Rubik
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private enum class Range(val days: Long) { WEEK(7), MONTH(30), QUARTER(90) }

/** Weight: the latest weigh-in and the way to the goal, the trend, and every weigh-in. */
@Composable
fun WeightScreen(
    onBack: () -> Unit,
    onLog: () -> Unit,
    onEdit: (Long) -> Unit,
    onGoal: () -> Unit,
    viewModel: WeightViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val goal by viewModel.goal.collectAsState()
    Column(Modifier.fillMaxSize().background(AppColors.ground)) {
        BodyHeader(
            stringResource(R.string.weight_title), onBack,
            action = R.drawable.ic_target, actionLabel = stringResource(R.string.body_nav_goal), onAction = onGoal
        )
        val list = entries
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = BodyGutter, end = BodyGutter, top = 4.dp, bottom = 16.dp)
        ) {
            if (list == null) return@LazyColumn
            item { CurrentCard(list, goal, onGoal) }
            if (list.isNotEmpty()) {
                item { Column { Gap(12); TrendCard(list); Gap(12) } }
                // One card, a row per weigh-in: the title is its top, the last row its bottom
                item { HistoryTitle() }
                itemsIndexed(list, key = { _, e -> e.id }) { i, e ->
                    HistoryRow(e, list.getOrNull(i + 1), last = i == list.lastIndex) { onEdit(e.id) }
                }
            }
        }
        BodyFooter(stringResource(R.string.weight_log), onLog, R.drawable.ic_home_plus)
    }
}

@Composable
private fun CurrentCard(entries: List<WeighIn>, goal: WeightGoal?, onGoal: () -> Unit) {
    val unit = LocalWeightUnit.current
    val locale = appLocale()
    val latest = entries.firstOrNull()
    BodyCard(padding = PaddingValues(horizontal = 20.dp, vertical = 18.dp), gap = 16.dp) {
        if (latest == null) {
            Text(stringResource(R.string.weight_none_yet), style = bodyTitle.copy(color = AppColors.textPrimary))
            Text(stringResource(R.string.weight_none_yet_text), style = bodyCaption)
            return@BodyCard
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.weight_current, dayName(latest.measuredAt)), style = bodyCaption)
                BigNumber(weightTenth(latest.kg), weightUnitLabel(), AppColors.accentStrength)
            }
            val startKg = goal?.startKg ?: entries.last().kg
            val change = latest.kg - startKg
            if (entries.size > 1 || goal != null) {
                // Towards the target is good; with no goal, down is
                val good = if (goal != null) abs(latest.kg - goal.targetKg) < abs(startKg - goal.targetKg) else change < 0
                Pill(
                    stringResource(R.string.weight_since_start, stringResource(R.string.units_value, unit.signed(change, locale), weightUnitLabel())),
                    if (good) AppColors.goodTint else AppColors.ground,
                    if (good) AppColors.goodText else AppColors.textSecondary
                )
            }
        }
        if (goal != null) {
            val progress = WeightStats.progress(goal, latest.kg)
            val left = abs(latest.kg - goal.targetKg)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(AppColors.border)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(AppColors.accentStrength)
                    )
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.weight_start, weightTenth(goal.startKg)), style = AppType.hint)
                    Text(
                        if (progress >= 1f) stringResource(R.string.weight_at_goal)
                        else stringResource(R.string.weight_to_go, (progress * 100).roundToInt(), weightTenthText(left)),
                        style = AppType.hint.copy(color = AppColors.textPrimary, fontWeight = FontWeight.Medium),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Text(stringResource(R.string.weight_goal_at, weightTenth(goal.targetKg)), style = AppType.hint)
                }
            }
        } else {
            Text(
                stringResource(R.string.weight_set_goal),
                style = AppType.smallButton.copy(color = AppColors.accentStrength),
                modifier = Modifier.clickable(role = Role.Button, onClick = onGoal).padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun TrendCard(entries: List<WeighIn>) {
    var range by rememberSaveable { mutableStateOf(Range.MONTH) }
    val unit = LocalWeightUnit.current
    val locale = appLocale()
    val now = System.currentTimeMillis()
    BodyCard(gap = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.weight_trend), style = bodyTitle.copy(color = AppColors.textPrimary), modifier = Modifier.weight(1f))
            Segmented(
                listOf(
                    Range.WEEK to stringResource(R.string.weight_range_week),
                    Range.MONTH to stringResource(R.string.weight_range_month),
                    Range.QUARTER to stringResource(R.string.weight_range_quarter)
                ),
                range, { range = it }, height = 32.dp, fill = false
            )
        }
        TrendChart(entries, range, now)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(line = true, stringResource(R.string.weight_trend_legend))
            Legend(line = false, stringResource(R.string.weight_points_legend))
        }
        val avg = WeightStats.weekAverage(entries, now)
        val change = WeightStats.weekChange(entries, now)
        Row(
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BodyInnerRadius))
                .background(AppColors.border)
        ) {
            Stat(avg?.let { unit.tenth(it, locale) } ?: "–", stringResource(R.string.weight_week_avg), AppColors.textPrimary)
            Stat(
                change?.let { unit.signed(it, locale) } ?: "–", stringResource(R.string.weight_week_change),
                if (change != null && change < -0.04) AppColors.goodText else AppColors.textPrimary
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Stat(value: String, label: String, color: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .weight(1f)
            .background(AppColors.surfaceMuted)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = AppType.button.copy(fontSize = 18.sp, color = color), modifier = Modifier.alignByBaseline())
            Text(weightUnitLabel(), style = AppType.hint.copy(fontWeight = FontWeight.Medium), modifier = Modifier.alignByBaseline())
        }
        Text(label, style = AppType.hint)
    }
}

@Composable
private fun Legend(line: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(if (line) 14.dp else 6.dp, if (line) 3.dp else 6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (line) AppColors.accentStrength else AppColors.weighInDot)
        )
        Text(text, style = AppType.hint)
    }
}

/**
 * The range's weigh-ins as dots and their 7-day trend as a line, over time; drawn left to right in
 * either language, as a timeline reads.
 */
@Composable
private fun TrendChart(entries: List<WeighIn>, range: Range, now: Long) {
    val unit = LocalWeightUnit.current
    val locale = appLocale()
    val zone = ZoneId.systemDefault()
    val from = LocalDate.now().minusDays(range.days - 1).atStartOfDay(zone).toInstant().toEpochMilli()
    val inRange = entries.filter { it.measuredAt >= from }.sortedBy { it.measuredAt }
    val trend = remember(entries) { WeightStats.trend(entries) }.filter { it.first >= from }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Rubik, fontSize = 11.sp, color = AppColors.textMuted)
    val startLabel = Instant.ofEpochMilli(from).atZone(zone).toLocalDate().format(DateTimeFormatter.ofPattern("d MMM", locale))
    val todayLabel = stringResource(R.string.weight_today)
    val description = stringResource(R.string.weight_trend)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = description }
    ) {
        val left = 34.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = 136.dp.toPx()
        val right = size.width - 4.dp.toPx()
        val values = (inRange.map { unit.of(it.kg.toFloat()) } + trend.map { unit.of(it.second.toFloat()) })
        var lo = values.minOrNull()?.let { floor((it - 0.2) * 2) / 2 } ?: 0.0
        var hi = values.maxOrNull()?.let { ceil((it + 0.2) * 2) / 2 } ?: 1.0
        if (hi - lo < 1) hi = lo + 1
        val x = { t: Long -> left + (t - from).toFloat() / (now - from).coerceAtLeast(1) * (right - left) }
        val y = { v: Double -> (top + (hi - v) / (hi - lo) * (bottom - top)).toFloat() }

        listOf(hi, (hi + lo) / 2, lo).forEach { v ->
            val yy = y(v)
            drawLine(AppColors.border, Offset(left, yy), Offset(right, yy), 1.dp.toPx())
            val label = measurer.measure("%.1f".format(locale, v), labelStyle)
            drawText(label, topLeft = Offset(left - 8.dp.toPx() - label.size.width, yy - label.size.height / 2f))
        }
        val start = measurer.measure(startLabel, labelStyle)
        drawText(start, topLeft = Offset(left, size.height - start.size.height))
        val end = measurer.measure(todayLabel, labelStyle)
        drawText(end, topLeft = Offset(right - end.size.width, size.height - end.size.height))
        if (inRange.isEmpty()) return@Canvas

        if (trend.isNotEmpty()) {
            val line = Path()
            trend.forEachIndexed { i, (t, kg) ->
                val px = x(t); val py = y(unit.of(kg.toFloat()))
                if (i == 0) line.moveTo(px, py) else line.lineTo(px, py)
            }
            val area = Path().apply {
                addPath(line)
                lineTo(x(trend.last().first), bottom)
                lineTo(x(trend.first().first), bottom)
                close()
            }
            drawPath(area, AppColors.accentStrength.copy(alpha = 0.08f))
            val r = when (range) { Range.QUARTER -> 1.4f; Range.MONTH -> 2.2f; Range.WEEK -> 3.5f }.dp.toPx()
            inRange.forEach { drawCircle(AppColors.weighInDot, r, Offset(x(it.measuredAt), y(unit.of(it.kg.toFloat())))) }
            drawPath(line, AppColors.accentStrength, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            val (lt, lkg) = trend.last()
            val last = Offset(x(lt), y(unit.of(lkg.toFloat())))
            drawCircle(Color.White, 5.dp.toPx(), last)
            drawCircle(AppColors.accentStrength, 5.dp.toPx(), last, style = Stroke(2.5.dp.toPx()))
        }
    }
}

@Composable
private fun HistoryTitle() {
    Text(
        stringResource(R.string.weight_history),
        style = bodyTitle.copy(color = AppColors.textPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = BodyCardRadius, topEnd = BodyCardRadius))
            .background(AppColors.surface)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
    )
}

@Composable
private fun HistoryRow(entry: WeighIn, before: WeighIn?, last: Boolean, onClick: () -> Unit) {
    val unit = LocalWeightUnit.current
    val locale = appLocale()
    val shape = if (last) RoundedCornerShape(bottomStart = BodyCardRadius, bottomEnd = BodyCardRadius) else RoundedCornerShape(0.dp)
    Column {
    Box(Modifier.fillMaxWidth().background(AppColors.surface)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(AppColors.border))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(start = 16.dp, end = 16.dp, bottom = if (last) 6.dp else 0.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(dayName(entry.measuredAt), style = AppType.menuItem)
            Text(dateAndTime(entry.measuredAt), style = AppType.hint)
        }
        if (before != null) {
            val d = entry.kg - before.kg
            val down = unit.of(d.toFloat()) <= -0.05
            val up = unit.of(d.toFloat()) >= 0.05
            Text(
                (if (down) "↓ " else if (up) "↑ " else "") + unit.signed(d, locale),
                style = AppType.caption.copy(fontWeight = FontWeight.Medium, color = if (down) AppColors.goodText else AppColors.textMuted)
            )
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(unit.tenth(entry.kg, locale), style = AppType.button.copy(color = AppColors.textPrimary), modifier = Modifier.alignByBaseline())
            Text(weightUnitLabel(), style = AppType.hint.copy(fontWeight = FontWeight.Medium), modifier = Modifier.alignByBaseline())
        }
    }
    }
}

/** "Today", "Yesterday", the weekday within the week, else the date. */
@Composable
private fun dayName(at: Long): String {
    val locale = appLocale()
    val day = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when {
        day == today -> stringResource(R.string.weight_today)
        day == today.minusDays(1) -> stringResource(R.string.weight_yesterday)
        day.isAfter(today.minusDays(7)) -> day.format(DateTimeFormatter.ofPattern("EEEE", locale))
        else -> day.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
    }
}

/** "9 Nov · 07:40". */
@Composable
private fun dateAndTime(at: Long): String {
    val context = LocalContext.current
    val t = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val clock = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return t.format(DateTimeFormatter.ofPattern("d MMM · $clock", appLocale()))
}

private fun WeightUnit.tenthsOf(kg: Double): Int = (of(kg.toFloat()) * 10).roundToInt()
/** A value on the scale back in kg, kept as a Double: through a Float, 83.3 would be saved as 83.30000305. */
private fun WeightUnit.kgOf(tenths: Int): Double = when (this) {
    WeightUnit.KG -> tenths / 10.0
    WeightUnit.LB -> tenths / 10.0 * com.doron.gymtracker.data.KG_PER_LB
}

/** A new weigh-in, or one changed: a ruler to drag, ±0.1 to fine-tune, and when it was. */
@Composable
fun WeightLogScreen(onDone: () -> Unit, viewModel: WeightLogViewModel = hiltViewModel()) {
    val start by viewModel.start.collectAsState()
    val s = start
    Column(Modifier.fillMaxSize().background(AppColors.ground)) {
        if (s == null) return@Column
        val unit = LocalWeightUnit.current
        val locale = appLocale()
        val minT = unit.tenthsOf(WeightStats.MIN_KG)
        val maxT = unit.tenthsOf(WeightStats.MAX_KG)
        var tenths by rememberSaveable { mutableIntStateOf(unit.tenthsOf(s.kg)) }
        var at by rememberSaveable { mutableLongStateOf(s.measuredAt) }
        var confirmDelete by remember { mutableStateOf(false) }
        val editing = viewModel.editingId != null
        val haptics = LocalHapticFeedback.current
        val set = { t: Int ->
            val c = t.coerceIn(minT, maxT)
            if (c != tenths) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            tenths = c
        }
        val value = "%.1f".format(locale, tenths / 10.0)

        BodyHeader(
            stringResource(if (editing) R.string.weight_edit else R.string.weight_log), onDone, close = true,
            action = if (editing) R.drawable.ic_trash else null,
            actionLabel = stringResource(R.string.weight_delete), onAction = { confirmDelete = true }
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BodyGutter, vertical = 8.dp)
        ) {
            WhenChip(at) { at = it }
            Gap(56)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    value,
                    style = AppType.cardTitle.copy(fontSize = 80.sp, lineHeight = 80.sp, letterSpacing = (-2).sp),
                    modifier = Modifier.alignByBaseline()
                )
                Text(
                    weightUnitLabel(),
                    style = AppType.cardTitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Medium, color = AppColors.textMuted),
                    modifier = Modifier.alignByBaseline()
                )
            }
            Gap(8)
            if (!editing && s.lastKg != null) {
                val d = unit.kgOf(tenths) - s.lastKg
                val down = tenths < unit.tenthsOf(s.lastKg)
                val same = tenths == unit.tenthsOf(s.lastKg)
                Pill(
                    if (same) stringResource(R.string.weight_same_as_last)
                    else stringResource(
                        R.string.weight_since_last,
                        (if (down) "↓ " else "↑ ") + stringResource(R.string.units_value, unit.signed(d, locale), weightUnitLabel())
                    ),
                    if (down) AppColors.goodTint else AppColors.border,
                    if (down) AppColors.goodText else AppColors.textSecondary
                )
            }
            Gap(40)
            Ruler(tenths, set)
            Gap(20)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StepButton(false, stringResource(R.string.weight_less), { set(tenths - 1) }, AppColors.surface)
                Text(
                    stringResource(R.string.weight_scale_hint), style = AppType.caption,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(150.dp)
                )
                StepButton(true, stringResource(R.string.weight_more), { set(tenths + 1) }, AppColors.surface)
            }
        }
        BodyFooter(
            stringResource(R.string.weight_save, stringResource(R.string.units_value, value, weightUnitLabel())),
            { viewModel.save(unit.kgOf(tenths), at, onDone) },
            R.drawable.ic_check
        )
        if (confirmDelete) {
            DesignDialog(
                title = stringResource(R.string.weight_delete),
                text = stringResource(R.string.weight_delete_text),
                onDismiss = { confirmDelete = false }
            ) {
                DesignButton(
                    stringResource(R.string.weight_delete), R.drawable.ic_trash, AppColors.dangerTint, AppColors.danger,
                    { confirmDelete = false; viewModel.delete(onDone) }, Modifier.fillMaxWidth()
                )
                DesignButton(
                    stringResource(R.string.body_cancel), R.drawable.ic_close, AppColors.ground, AppColors.textPrimary,
                    { confirmDelete = false }, Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** When the weigh-in was: today and the time; tapping picks the day, then the time. */
@Composable
private fun WhenChip(at: Long, onChange: (Long) -> Unit) {
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    val t = Instant.ofEpochMilli(at).atZone(zone).toLocalDateTime()
    val label = "${dayName(at)} · ${clockText(t.hour * 60 + t.minute)}"
    val description = stringResource(R.string.weight_when)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(AppColors.surface)
            .clickable(role = Role.Button) {
                DatePickerDialog(context, { _, y, m, d ->
                    TimePickerDialog(context, { _, h, min ->
                        val picked = LocalDateTime.of(y, m + 1, d, h, min)
                        // Not later than now: a weigh-in that hasn't happened yet would lead the trend
                        val now = LocalDateTime.now()
                        onChange((if (picked.isAfter(now)) now else picked).atZone(zone).toInstant().toEpochMilli())
                    }, t.hour, t.minute, DateFormat.is24HourFormat(context)).show()
                }, t.year, t.monthValue - 1, t.dayOfMonth).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            }
            .semantics { contentDescription = "$description: $label" }
            .padding(horizontal = 14.dp)
    ) {
        Icon(painterResource(R.drawable.ic_home_calendar), contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(16.dp))
        Text(label, style = AppType.smallButton.copy(fontWeight = FontWeight.Medium, color = AppColors.textPrimary))
        Icon(painterResource(R.drawable.ic_chevron_down), contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
    }
}

/**
 * The scale: a tenth per tick, a long one each whole number. Dragging moves it under the fixed
 * needle; it stays left to right in Hebrew too, as numbers on a ruler read.
 */
@Composable
private fun Ruler(tenths: Int, onChange: (Int) -> Unit) {
    val density = LocalDensity.current
    val tickPx = with(density) { 9.dp.toPx() }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Rubik, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = AppColors.textMuted)
    val locale = appLocale()
    // Where the drag has got to, in tenths with their fraction, so slow drags still add up
    var dragged by remember { mutableFloatStateOf(0f) }
    val current by androidx.compose.runtime.rememberUpdatedState(tenths)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(BodyCardRadius))
            .background(AppColors.surface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragged = current.toFloat() },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        dragged -= dx / tickPx
                        onChange(dragged.roundToInt())
                    }
                )
            }
    ) {
        val centre = size.width / 2
        val span = (centre / tickPx).toInt() + 2
        for (k in tenths - span..tenths + span) {
            if (k < 0) continue
            val x = centre + (k - tenths) * tickPx
            val major = k % 10 == 0
            val h = (if (major) 46 else if (k % 5 == 0) 32 else 20).dp.toPx()
            drawRoundRect(
                if (major) AppColors.textPrimary else AppColors.rulerTick,
                topLeft = Offset(x - 1.dp.toPx(), 14.dp.toPx()), size = Size(2.dp.toPx(), h), cornerRadius = CornerRadius(1.dp.toPx())
            )
            if (major) {
                val label = measurer.measure(String.format(locale, "%d", k / 10), labelStyle)
                drawText(label, topLeft = Offset(x - label.size.width / 2f, 70.dp.toPx()))
            }
        }
        val fade = 64.dp.toPx()
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), 0f, fade), size = Size(fade, size.height))
        drawRect(
            Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White), size.width - fade, size.width),
            topLeft = Offset(size.width - fade, 0f), size = Size(fade, size.height)
        )
        drawRoundRect(
            AppColors.accentStrength, topLeft = Offset(centre - 2.dp.toPx(), 6.dp.toPx()),
            size = Size(4.dp.toPx(), 68.dp.toPx()), cornerRadius = CornerRadius(2.dp.toPx())
        )
        val tip = Path().apply {
            moveTo(centre - 7.dp.toPx(), 0f)
            lineTo(centre + 7.dp.toPx(), 0f)
            lineTo(centre, 8.dp.toPx())
            close()
        }
        drawPath(tip, AppColors.accentStrength)
    }
}

/** The weight goal: the target, the weekly pace and when it's reached; and the weigh-in reminder. */
@Composable
fun WeightGoalScreen(
    onDone: () -> Unit,
    viewModel: WeightGoalViewModel = hiltViewModel(),
    settings: BodySettingsViewModel = hiltViewModel()
) {
    val start by viewModel.start.collectAsState()
    val reminder by settings.weightReminder.collectAsState()
    val s = start
    Column(Modifier.fillMaxSize().background(AppColors.ground)) {
        BodyHeader(stringResource(R.string.weight_goal_title), onDone)
        if (s == null) return@Column
        val unit = LocalWeightUnit.current
        val locale = appLocale()
        // Half steps in the person's unit
        val currentT = unit.tenthsOf(s.currentKg)
        var targetT by rememberSaveable {
            mutableIntStateOf(s.goal?.let { unit.tenthsOf(it.targetKg) } ?: ((currentT - 50) / 5 * 5))
        }
        val paces = listOf(0.25, 0.5, 0.75)
        var pace by rememberSaveable { mutableStateOf(paces.minBy { p -> abs(p - (s.goal?.paceKgPerWeek ?: 0.5)) }) }
        val minT = unit.tenthsOf(WeightStats.MIN_KG)
        val maxT = unit.tenthsOf(WeightStats.MAX_KG)
        val targetKg = unit.kgOf(targetT)

        Column(
            verticalArrangement = Arrangement.spacedBy(BodyGap),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = BodyGutter, end = BodyGutter, top = 4.dp, bottom = 16.dp)
        ) {
            BodyCard(padding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), gap = 16.dp) {
                Text(stringResource(R.string.weight_target), style = bodyTitle.copy(color = AppColors.textPrimary))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    StepButton(false, stringResource(R.string.weight_target_lower), { targetT = (targetT - 5).coerceAtLeast(minT) })
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BigNumber("%.1f".format(locale, targetT / 10.0), weightUnitLabel(), AppColors.accentStrength)
                        val diff = abs(targetT - currentT) / 10.0
                        val diffText = stringResource(R.string.units_value, "%.1f".format(locale, diff), weightUnitLabel())
                        Text(
                            when {
                                targetT < currentT -> stringResource(R.string.weight_below_today, diffText)
                                targetT > currentT -> stringResource(R.string.weight_above_today, diffText)
                                else -> stringResource(R.string.weight_same_as_today)
                            },
                            style = bodyCaption
                        )
                    }
                    StepButton(true, stringResource(R.string.weight_target_higher), { targetT = (targetT + 5).coerceAtMost(maxT) })
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.border))
                Text(stringResource(R.string.weight_pace), style = bodyCaption)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(R.string.weight_pace_relaxed, R.string.weight_pace_steady, R.string.weight_pace_faster).zip(paces).forEach { (name, p) ->
                        PaceOption(stringResource(name), stringResource(R.string.weight_pace_amount, stripZeros(unit, p, locale)), p == pace) { pace = p }
                    }
                }
                val weeks = WeightStats.weeksToGo(s.currentKg, targetKg, pace)
                Text(
                    if (weeks == 0) stringResource(R.string.weight_at_target)
                    else pluralStringResource(R.plurals.weight_eta, weeks, weightTenthText(targetKg), weeks),
                    style = AppType.menuItem.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BodyInnerRadius))
                        .background(AppColors.ground)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
            BodyCard(padding = PaddingValues(horizontal = 16.dp, vertical = 18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.body_weight_reminder), style = bodyTitle.copy(color = AppColors.textPrimary))
                    Text(stringResource(R.string.reminder_weight_text), style = bodyCaption)
                }
                WeightReminderControls(reminder, settings::setWeightReminder)
            }
        }
        BodyFooter(stringResource(R.string.weight_save_goal), { viewModel.save(targetKg, pace, onDone) })
    }
}

/** A pace in the person's unit, as short as it reads: "0.25 kg", "0.6 lb". */
@Composable
private fun stripZeros(unit: WeightUnit, kg: Double, locale: Locale): String {
    val v = unit.of(kg.toFloat())
    val text = if (unit == WeightUnit.KG) "%.2f".format(locale, v).trimEnd('0').trimEnd('.', ',') else "%.1f".format(locale, v)
    return stringResource(R.string.units_value, text, weightUnitLabel())
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.PaceOption(name: String, amount: String, on: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(BodyInnerRadius))
            .background(if (on) AppColors.tintStrength else AppColors.surface)
            .border(if (on) 2.dp else 1.dp, if (on) AppColors.accentStrength else AppColors.border, RoundedCornerShape(BodyInnerRadius))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
    ) {
        Text(name, style = AppType.smallButton.copy(color = if (on) AppColors.accentStrength else AppColors.textPrimary))
        Text(amount, style = AppType.hint.copy(color = if (on) AppColors.accentStrength else AppColors.textMuted))
    }
}
