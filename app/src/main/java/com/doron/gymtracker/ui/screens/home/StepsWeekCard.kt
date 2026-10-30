package com.doron.gymtracker.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * This week's steps: a bar a day against the daily goal, the part taken on runs and walks
 * in green at its foot. Touch a day to read its count.
 */
@Composable
fun StepsWeekCard(week: StepsWeek, modifier: Modifier = Modifier) {
    var selected by remember(week) { mutableStateOf<Int?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val todayStyle = labelStyle.copy(color = Purple, fontWeight = FontWeight.Bold)
    val goalStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    val belowGoal = Purple.copy(alpha = 0.3f)
    val hasWorkouts = week.days.any { it.workoutSteps > 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(surface, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        val day = selected?.let { week.days[it] }
        CardTitle(
            "👣 צעדים השבוע",
            trailing = if (day != null) {
                val name = day.date.dayOfWeek.getDisplayName(DateTextStyle.FULL, Locale("he"))
                val ofWorkout = if (day.workoutSteps > 0) " (%,d באימון)".format(day.workoutSteps) else ""
                "$name · %,d$ofWorkout".format(day.steps)
            } else "ממוצע %,d".format(week.average)
        )
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .pointerInput(week) {
                    detectTapGestures { pos ->
                        val col = (pos.x / (size.width / 7f)).toInt().coerceIn(0, 6)
                        val i = if (rtl) 6 - col else col
                        selected = if (selected == i || week.days[i].isFuture) null else i
                    }
                }
        ) {
            val labelH = 20.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - labelH
            val colW = size.width / 7f
            val barW = colW * 0.46f
            val max = maxOf(week.days.maxOf { it.steps }, week.goal.toLong()) * 1.1f
            fun y(steps: Long) = bottom - (steps / max) * (bottom - top)
            // Sunday sits at the start of the row, as in the calendar: the right in Hebrew
            fun centerX(i: Int) = colW * ((if (rtl) 6 - i else i) + 0.5f)

            week.days.forEachIndexed { i, day ->
                val cx = centerX(i)
                val left = cx - barW / 2
                if (day.steps > 0) {
                    val color = if (day.steps >= week.goal) Purple else belowGoal
                    roundedTopBar(color, left, y(day.steps), barW, bottom)
                    if (day.workoutSteps > 0) {
                        // The workout part, cut from the rest of the bar by a thin gap
                        val wTop = y(day.workoutSteps)
                        val gap = 2.dp.toPx()
                        val reachesTop = day.workoutSteps >= day.steps
                        if (reachesTop) roundedTopBar(SuccessGreen, left, wTop, barW, bottom)
                        else {
                            drawRect(SuccessGreen, Offset(left, wTop), Size(barW, bottom - wTop))
                            drawRect(surface, Offset(left, wTop - gap), Size(barW, gap))
                        }
                    }
                }
                if (selected == i) {
                    drawRoundRect(
                        Purple, Offset(left - 3.dp.toPx(), y(day.steps) - 3.dp.toPx()),
                        Size(barW + 6.dp.toPx(), bottom - y(day.steps) + 3.dp.toPx()),
                        CornerRadius(6.dp.toPx()), style = Stroke(1.5.dp.toPx())
                    )
                }
                val label = textMeasurer.measure(
                    day.date.dayOfWeek.getDisplayName(DateTextStyle.NARROW, Locale("he")),
                    if (day.isToday) todayStyle else labelStyle
                )
                drawText(label, topLeft = Offset(cx - label.size.width / 2, bottom + 4.dp.toPx()))
            }

            // Goal line, with its number at the end
            val gy = y(week.goal.toLong())
            drawLine(
                gridColor, Offset(0f, gy), Offset(size.width, gy), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
            val goalLabel = textMeasurer.measure(compact(week.goal.toLong()), goalStyle)
            val gx = if (rtl) 0f else size.width - goalLabel.size.width
            drawRect(surface, Offset(gx, gy - goalLabel.size.height / 2), Size(goalLabel.size.width.toFloat(), goalLabel.size.height.toFloat()))
            drawText(goalLabel, topLeft = Offset(gx, gy - goalLabel.size.height / 2))
        }

        Spacer(Modifier.height(8.dp))
        val change = week.changePercent?.let {
            when {
                it > 0 -> " · ↑$it% משבוע שעבר"
                it < 0 -> " · ↓${abs(it)}% משבוע שעבר"
                else -> " · כמו בשבוע שעבר"
            }
        } ?: ""
        Text(
            "סה״כ %,d · %d %s ביעד".format(week.total, week.daysAtGoal, if (week.daysAtGoal == 1) "יום" else "ימים") + change,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendDot(Purple, "ביעד")
            LegendDot(belowGoal, "מתחת ליעד")
            if (hasWorkouts) LegendDot(SuccessGreen, "ריצה / הליכה")
        }
    }
}

/** The title every home card with one shares: bold at the start, an optional summary at the end. */
@Composable
fun CardTitle(title: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A bar anchored to the baseline, rounded only at its top. */
private fun DrawScope.roundedTopBar(color: Color, left: Float, top: Float, width: Float, bottom: Float) {
    val r = minOf(4.dp.toPx(), (bottom - top) / 2, width / 2)
    val path = Path().apply {
        moveTo(left, bottom)
        lineTo(left, top + r)
        quadraticTo(left, top, left + r, top)
        lineTo(left + width - r, top)
        quadraticTo(left + width, top, left + width, top + r)
        lineTo(left + width, bottom)
        close()
    }
    drawPath(path, color)
}

/** 10,000 → "10K", 7,500 → "7.5K". */
private fun compact(steps: Long): String =
    if (steps % 1000 == 0L) "${steps / 1000}K" else "%.1fK".format(steps / 1000.0)
