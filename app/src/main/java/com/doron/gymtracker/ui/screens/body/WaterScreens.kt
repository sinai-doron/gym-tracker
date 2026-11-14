package com.doron.gymtracker.ui.screens.body

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.WaterReminder
import com.doron.gymtracker.domain.model.Drink
import com.doron.gymtracker.domain.model.Reminders
import com.doron.gymtracker.domain.model.WaterGoal
import com.doron.gymtracker.ui.components.appLocale
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppType
import com.doron.gymtracker.ui.theme.Rubik
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/** The most a drink can be, as the server takes it. */
private const val MAX_DRINK = 5_000

/** Water today: the bottle filling to the goal, quick adds, today's drinks and the last 7 days. */
@Composable
fun WaterScreen(onBack: () -> Unit, onGoal: () -> Unit, viewModel: WaterViewModel = hiltViewModel()) {
    val week by viewModel.week.collectAsState()
    val goal by viewModel.goal.collectAsState()
    val reminder by viewModel.reminder.collectAsState()
    val locale = appLocale()
    val zone = ZoneId.systemDefault()
    val todayStart = Reminders.startOfDay(LocalDate.now(), zone)
    val drinks = week ?: emptyList()
    val today = drinks.filter { it.drankAt >= todayStart }
    val total = today.sumOf { it.ml }
    var otherOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppColors.ground)) {
        BodyHeader(
            stringResource(R.string.water_title), onBack,
            subtitle = stringResource(R.string.water_today_date, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMM", locale))),
            action = R.drawable.ic_target, actionLabel = stringResource(R.string.body_nav_goal), onAction = onGoal
        )
        if (week == null) return@Column
        Column(
            verticalArrangement = Arrangement.spacedBy(BodyGap),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = BodyGutter, end = BodyGutter, top = 4.dp, bottom = 24.dp)
        ) {
            IntakeCard(total, goal, reminder)
            Text(stringResource(R.string.water_add), style = bodyTitle.copy(color = AppColors.textPrimary), modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AddTile(R.drawable.ic_glass, stringResource(R.string.water_glass), stringResource(R.string.water_ml, mlText(goal.glassMl, locale))) { viewModel.add(goal.glassMl) }
                AddTile(R.drawable.ic_bottle, stringResource(R.string.water_bottle), stringResource(R.string.water_ml, mlText(goal.bottleMl, locale))) { viewModel.add(goal.bottleMl) }
                AddTile(R.drawable.ic_home_plus, stringResource(R.string.water_other), stringResource(R.string.water_any_amount), dashed = true) { otherOpen = true }
            }
            TodayLog(today, goal, viewModel::remove)
            WeekCard(drinks, goal, zone)
        }
    }
    if (otherOpen) {
        AmountDialog(
            title = stringResource(R.string.water_other_title),
            initial = 300, min = 50, max = MAX_DRINK, step = 50,
            confirm = { stringResource(R.string.water_add_ml, mlText(it, locale)) },
            onConfirm = { viewModel.add(it); otherOpen = false },
            onDismiss = { otherOpen = false }
        )
    }
}

@Composable
private fun IntakeCard(total: Int, goal: WaterGoal, reminder: WaterReminder) {
    val locale = appLocale()
    val fraction = if (goal.dailyMl > 0) total.toFloat() / goal.dailyMl else 0f
    val pct = (fraction * 100).roundToInt()
    BodyCard(padding = PaddingValues(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Bottle(fraction.coerceIn(0f, 1f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        mlText(total, locale),
                        style = AppType.cardTitle.copy(fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-1).sp)
                    )
                    Text(stringResource(R.string.water_of_goal, mlText(goal.dailyMl, locale), pct), style = AppType.caption.copy(fontSize = 14.sp))
                }
                if (total >= goal.dailyMl) {
                    Pill(stringResource(R.string.water_goal_reached), AppColors.goodTint, AppColors.goodText, R.drawable.ic_check)
                } else {
                    Text(
                        stringResource(R.string.water_left, mlText(goal.dailyMl - total, locale)),
                        style = AppType.smallButton.copy(fontWeight = FontWeight.Medium, color = AppColors.waterText)
                    )
                }
                Reminders.nextWater(reminder, LocalDateTime.now())?.let { next ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(R.drawable.ic_bell), contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(14.dp))
                        Text(stringResource(R.string.water_next_reminder, clockText(next.hour * 60 + next.minute)), style = AppType.caption)
                    }
                }
            }
        }
    }
}

// The design's bottle, in its 120 × 200 box
private const val BOTTLE = "M45 8 h30 v18 c0 8 25 14 25 34 v120 a12 12 0 0 1 -12 12 h-56 a12 12 0 0 1 -12 -12 v-120 c0 -20 25 -26 25 -34 z"

/** A bottle filled to [fraction], the water's surface a gentle wave. */
@Composable
private fun Bottle(fraction: Float) {
    val level by animateFloatAsState(fraction, tween(600), label = "water")
    val outline = remember { PathParser().parsePathString(BOTTLE).toPath() }
    Canvas(Modifier.size(96.dp, 160.dp)) {
        val s = size.width / 120f
        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
            clipPath(outline) {
                if (level > 0f) {
                    val surface = 192f - level * 184f
                    drawPath(wave(surface - 3f, phase = 1.2f), AppColors.waterBack)
                    drawPath(wave(surface, phase = 0f), AppColors.water)
                }
            }
            drawPath(outline, AppColors.textPrimary, style = Stroke(2.5f, join = StrokeJoin.Round))
            listOf(78f, 116f, 154f).forEach {
                drawLine(AppColors.textPrimary.copy(alpha = 0.35f), Offset(88f, it), Offset(96f, it), 2f, cap = StrokeCap.Round)
            }
        }
    }
}

private fun wave(y: Float, phase: Float): Path = Path().apply {
    moveTo(0f, 200f)
    lineTo(0f, y)
    var x = 0f
    while (x <= 120f) {
        lineTo(x, y + 3f * sin(x / 120f * 2 * PI.toFloat() * 1.5f + phase))
        x += 4f
    }
    lineTo(120f, 200f)
    close()
}

@Composable
private fun RowScope.AddTile(@DrawableRes icon: Int, title: String, amount: String, dashed: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 96.dp)
            .clip(shape)
            .then(if (dashed) Modifier.dashedBorder() else Modifier.background(AppColors.surface))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = if (dashed) AppColors.textMuted else AppColors.water, modifier = Modifier.size(26.dp))
        Text(title, style = AppType.smallButton.copy(color = AppColors.textPrimary))
        Text(amount, style = AppType.hint, textAlign = TextAlign.Center)
    }
}

/** The "any amount" tile's dashed outline. */
private fun Modifier.dashedBorder(): Modifier = this.then(
    Modifier.drawBehind {
        val w = 2.dp.toPx()
        drawRoundRect(
            AppColors.dashed,
            topLeft = Offset(w / 2, w / 2),
            size = Size(size.width - w, size.height - w),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx()),
            style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
        )
    }
)

@Composable
private fun TodayLog(today: List<Drink>, goal: WaterGoal, onRemove: (Long) -> Unit) {
    val locale = appLocale()
    val zone = ZoneId.systemDefault()
    BodyCard(padding = PaddingValues(top = 16.dp, bottom = 6.dp), gap = 0.dp) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 6.dp)) {
            Text(stringResource(R.string.water_today_log), style = bodyTitle.copy(color = AppColors.textPrimary), modifier = Modifier.weight(1f))
            if (today.isNotEmpty()) Text(pluralStringResource(R.plurals.water_drinks, today.size, today.size), style = AppType.caption)
        }
        if (today.isEmpty()) {
            Divider()
            Text(stringResource(R.string.water_nothing_yet), style = AppType.caption, modifier = Modifier.padding(16.dp))
        }
        today.forEach { d ->
            val t = Instant.ofEpochMilli(d.drankAt).atZone(zone).toLocalTime()
            val time = clockText(t.hour * 60 + t.minute)
            val removeLabel = stringResource(R.string.water_remove, mlText(d.ml, locale), time)
            Divider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(start = 16.dp, end = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(AppColors.waterTint)
                ) {
                    Icon(painterResource(R.drawable.ic_water_drop), contentDescription = null, tint = AppColors.water, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(stringResource(R.string.water_ml, mlText(d.ml, locale)), style = AppType.menuItem)
                    Text(
                        stringResource(
                            when (d.ml) {
                                goal.glassMl -> R.string.water_glass
                                goal.bottleMl -> R.string.water_bottle
                                else -> R.string.water_other
                            }
                        ),
                        style = AppType.hint
                    )
                }
                Text(time, style = AppType.caption)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onRemove(d.id) }
                        .semantics { contentDescription = removeLabel }
                ) {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = AppColors.textMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.border))

/** The last 7 days' totals as bars, today's last; the goal a dashed line across. */
@Composable
private fun WeekCard(drinks: List<Drink>, goal: WaterGoal, zone: ZoneId) {
    val locale = appLocale()
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val totals = days.map { day ->
        val from = Reminders.startOfDay(day, zone)
        val until = Reminders.startOfDay(day.plusDays(1), zone)
        drinks.filter { it.drankAt in from until until }.sumOf { it.ml }
    }
    val met = totals.count { it >= goal.dailyMl }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontFamily = Rubik, fontSize = 11.sp, color = AppColors.textMuted)
    val todayStyle = labelStyle.copy(color = AppColors.textPrimary, fontWeight = FontWeight.SemiBold)
    val todayLabel = stringResource(R.string.weight_today)
    val goalLabel = stringResource(R.string.water_goal_line, mlText(goal.dailyMl, locale))
    val names = days.map { if (it == today) todayLabel else it.format(DateTimeFormatter.ofPattern("EEE", locale)) }
    val rtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
    val description = stringResource(R.string.water_goal_hit, met)

    BodyCard(gap = 10.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.water_last_7_days), style = bodyTitle.copy(color = AppColors.textPrimary), modifier = Modifier.weight(1f))
            Text(description, style = AppType.caption)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(136.dp)
                .semantics { contentDescription = description }
        ) {
            val top = 26.dp.toPx()
            val bottom = 110.dp.toPx()
            val most = maxOf(goal.dailyMl * 1.15f, (totals.maxOrNull() ?: 0) * 1.05f)
            val y = { ml: Float -> bottom - ml / most * (bottom - top) }
            val slot = size.width / 7
            val barW = 28.dp.toPx().coerceAtMost(slot * 0.7f)
            val goalY = y(goal.dailyMl.toFloat())
            drawLine(
                AppColors.textMuted.copy(alpha = 0.6f), Offset(0f, goalY), Offset(size.width, goalY), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            )
            val g = measurer.measure(goalLabel, labelStyle)
            drawText(g, topLeft = Offset(if (rtl) 0f else size.width - g.size.width, goalY - g.size.height - 2.dp.toPx()))
            totals.forEachIndexed { i, ml ->
                // Days run the way the language reads: today at the end
                val col = if (rtl) 6 - i else i
                val cx = slot * col + slot / 2
                if (ml > 0) {
                    val h = (bottom - y(ml.toFloat())).coerceAtLeast(6.dp.toPx())
                    drawRoundRect(
                        if (ml >= goal.dailyMl || i == 6) AppColors.water else AppColors.waterMissed,
                        topLeft = Offset(cx - barW / 2, bottom - h), size = Size(barW, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )
                }
                val label = measurer.measure(names[i], if (i == 6) todayStyle else labelStyle)
                drawText(label, topLeft = Offset(cx - label.size.width / 2f, bottom + 8.dp.toPx()))
            }
        }
    }
}

/** A dialog to pick an amount with − and +, then confirm. */
@Composable
internal fun AmountDialog(
    title: String,
    initial: Int,
    min: Int,
    max: Int,
    step: Int,
    confirm: @Composable (Int) -> String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = appLocale()
    var ml by rememberSaveable { mutableIntStateOf(initial.coerceIn(min, max)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AppColors.surface)
                .padding(22.dp)
        ) {
            Text(title, style = AppType.headerTitle, color = AppColors.textPrimary, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                StepButton(false, stringResource(R.string.weight_less), { ml = (ml - step).coerceAtLeast(min) })
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    BigNumber(mlText(ml, locale), stringResource(R.string.units_ml), AppColors.textPrimary, size = 40, unitSize = 18, unitColor = AppColors.textMuted)
                }
                StepButton(true, stringResource(R.string.weight_more), { ml = (ml + step).coerceAtMost(max) })
            }
            BodyButton(confirm(ml), { onConfirm(ml) }, R.drawable.ic_check)
        }
    }
}

/** The water goal: the daily amount (with one from the weight), the quick-add sizes, and the reminders. */
@Composable
fun WaterGoalScreen(
    onDone: () -> Unit,
    viewModel: WaterGoalViewModel = hiltViewModel(),
    settings: BodySettingsViewModel = hiltViewModel()
) {
    val start by viewModel.start.collectAsState()
    val reminder by settings.waterReminder.collectAsState()
    val s = start
    Column(Modifier.fillMaxSize().background(AppColors.ground)) {
        BodyHeader(stringResource(R.string.water_goal_title), onDone)
        if (s == null) return@Column
        val locale = appLocale()
        var daily by rememberSaveable { mutableIntStateOf(s.goal.dailyMl) }
        var glass by rememberSaveable { mutableIntStateOf(s.goal.glassMl) }
        var bottle by rememberSaveable { mutableIntStateOf(s.goal.bottleMl) }
        var editing by rememberSaveable { mutableStateOf<String?>(null) }

        Column(
            verticalArrangement = Arrangement.spacedBy(BodyGap),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = BodyGutter, end = BodyGutter, top = 4.dp, bottom = 16.dp)
        ) {
            BodyCard(padding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), gap = 16.dp) {
                Text(stringResource(R.string.water_daily_goal), style = bodyTitle.copy(color = AppColors.textPrimary))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    StepButton(false, stringResource(R.string.water_goal_lower), { daily = (daily - 250).coerceAtLeast(WaterGoal.MIN_DAILY) })
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        BigNumber(mlText(daily, locale), stringResource(R.string.units_ml), AppColors.textPrimary, size = 48, unitSize = 18, unitColor = AppColors.textMuted)
                        val glasses = daily.toFloat() / glass
                        val text = if (glasses % 1f < 0.05f) "%d".format(locale, glasses.toInt()) else "%.1f".format(locale, glasses)
                        Text(stringResource(R.string.water_glasses, text), style = bodyCaption)
                    }
                    StepButton(true, stringResource(R.string.water_goal_higher), { daily = (daily + 250).coerceAtMost(WaterGoal.MAX_DAILY) })
                }
                Glasses(daily, glass)
                s.weightKg?.let { kg ->
                    val suggested = WaterGoal.suggestedFor(kg)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BodyInnerRadius))
                            .background(AppColors.waterTint)
                            .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(R.string.water_suggested, mlText(suggested, locale)), style = AppType.smallButton.copy(color = AppColors.textPrimary))
                            Text(stringResource(R.string.water_suggested_why, weightTenthText(kg)), style = AppType.hint.copy(color = AppColors.textSecondary))
                        }
                        Text(
                            stringResource(R.string.water_use),
                            style = AppType.smallButton.copy(color = AppColors.waterText),
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppColors.surface)
                                .clickable(role = Role.Button) { daily = suggested }
                                .padding(horizontal = 14.dp, vertical = 11.dp)
                        )
                    }
                }
            }
            BodyCard(padding = PaddingValues(top = 16.dp, bottom = 6.dp), gap = 0.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                    Text(stringResource(R.string.water_sizes), style = bodyTitle.copy(color = AppColors.textPrimary))
                    Text(stringResource(R.string.water_sizes_hint), style = bodyCaption)
                }
                SizeRow(R.drawable.ic_glass, stringResource(R.string.water_glass), glass) { editing = "glass" }
                SizeRow(R.drawable.ic_bottle, stringResource(R.string.water_bottle), bottle) { editing = "bottle" }
            }
            BodyCard {
                WaterReminderControls(reminder, settings::setWaterReminder)
            }
        }
        BodyFooter(stringResource(R.string.water_save), { viewModel.save(WaterGoal(daily, glass, bottle), onDone) })

        when (editing) {
            "glass" -> AmountDialog(
                stringResource(R.string.water_glass), glass, 50, 2_000, 50,
                { stringResource(R.string.water_save) }, { glass = it; editing = null }, { editing = null }
            )
            "bottle" -> AmountDialog(
                stringResource(R.string.water_bottle), bottle, 100, 3_000, 50,
                { stringResource(R.string.water_save) }, { bottle = it; editing = null }, { editing = null }
            )
        }
    }
}

/** The goal as glasses: one per glass, the last part-filled; at most two rows. */
@Composable
private fun Glasses(dailyMl: Int, glassMl: Int) {
    val count = ceil(dailyMl.toFloat() / glassMl).toInt().coerceIn(1, 24)
    val last = (dailyMl.toFloat() / glassMl) % 1f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (0 until count).chunked(12).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                (0 until 12).forEach { i ->
                    val index = row.firstOrNull()?.plus(i) ?: 0
                    val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                    Box(Modifier.weight(1f).height(22.dp)) {
                        if (index < count) {
                            val part = if (index == count - 1 && last > 0.05f) last else 1f
                            Box(Modifier.fillMaxSize().clip(shape).background(AppColors.waterTint))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height((22 * part).dp)
                                    .align(Alignment.BottomCenter)
                                    .clip(shape)
                                    .background(AppColors.water)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SizeRow(@DrawableRes icon: Int, label: String, ml: Int, onClick: () -> Unit) {
    val locale = appLocale()
    Divider()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp)
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = AppColors.water, modifier = Modifier.size(20.dp))
        Text(label, style = AppType.menuItem.copy(fontWeight = FontWeight.Normal), modifier = Modifier.weight(1f))
        Text(stringResource(R.string.water_ml, mlText(ml, locale)), style = AppType.menuItem.copy(fontWeight = FontWeight.SemiBold))
    }
}
