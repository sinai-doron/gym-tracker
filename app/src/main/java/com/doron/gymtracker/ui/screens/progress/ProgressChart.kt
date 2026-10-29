package com.doron.gymtracker.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
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
import com.doron.gymtracker.domain.model.ProgressPoint
import com.doron.gymtracker.ui.theme.Purple
import kotlin.math.abs

/**
 * Single-series line chart of the best value per session, time on the x axis (left to right,
 * also in RTL). Tap a point to read its date and value; the latest point is selected by default.
 */
@Composable
fun ProgressChart(
    points: List<ProgressPoint>,
    formatValue: (Float) -> String,
    formatDate: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var selected by remember(points) { mutableIntStateOf(points.lastIndex) }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)

    Column(modifier) {
        points.getOrNull(selected)?.let { p ->
            Text(
                "${formatDate(p.date)} · ${formatValue(p.value)}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .pointerInput(points) {
                        detectTapGestures { tap ->
                            val xs = xPositions(points, size.width.toFloat(), 56.dp.toPx(), 8.dp.toPx())
                            selected = xs.indices.minByOrNull { abs(xs[it] - tap.x) } ?: selected
                        }
                    }
            ) {
                val left = 56.dp.toPx()
                val right = 8.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 20.dp.toPx()
                val minV = points.minOf { it.value }
                val maxV = points.maxOf { it.value }
                // Pad the range so a flat line sits mid-chart rather than on an edge
                val pad = if (maxV == minV) maxOf(1f, maxV * 0.1f) else (maxV - minV) * 0.15f
                val lo = minV - pad
                val hi = maxV + pad
                fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
                val xs = xPositions(points, size.width, left, right)

                // Recessive grid: three lines with their values
                listOf(minV, (minV + maxV) / 2, maxV).distinct().forEach { v ->
                    drawLine(axisColor, Offset(left, y(v)), Offset(size.width - right, y(v)), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    val label = textMeasurer.measure(formatValue(v), labelStyle)
                    drawText(label, topLeft = Offset(0f, y(v) - label.size.height / 2))
                }
                // First and last dates under the axis
                val firstDate = textMeasurer.measure(formatDate(points.first().date), labelStyle)
                // A lone point sits mid-chart, so its date goes under it
                val firstDateX = if (points.size == 1) xs[0] - firstDate.size.width / 2 else left
                drawText(firstDate, topLeft = Offset(firstDateX, bottom + 4.dp.toPx()))
                if (points.size > 1) {
                    val lastDate = textMeasurer.measure(formatDate(points.last().date), labelStyle)
                    drawText(lastDate, topLeft = Offset(size.width - right - lastDate.size.width, bottom + 4.dp.toPx()))
                }

                if (points.size > 1) {
                    val path = Path().apply {
                        points.forEachIndexed { i, p ->
                            if (i == 0) moveTo(xs[i], y(p.value)) else lineTo(xs[i], y(p.value))
                        }
                    }
                    drawPath(path, Purple, style = Stroke(width = 2.dp.toPx()))
                }
                points.forEachIndexed { i, p ->
                    val c = Offset(xs[i], y(p.value))
                    if (i == selected) {
                        drawLine(axisColor, Offset(c.x, top), Offset(c.x, bottom), 1.dp.toPx())
                    }
                    val r = if (i == selected) 6.dp.toPx() else 4.dp.toPx()
                    // Surface ring keeps markers legible where they sit on the line
                    drawCircle(surface, r + 2.dp.toPx(), c)
                    drawCircle(Purple, r, c)
                }
            }
        }
    }
}

private fun xPositions(points: List<ProgressPoint>, width: Float, left: Float, right: Float): List<Float> {
    if (points.size == 1) return listOf((left + width - right) / 2)
    val t0 = points.first().date.toFloat()
    val span = (points.last().date - points.first().date).toFloat().coerceAtLeast(1f)
    return points.map { left + (it.date - t0) / span * (width - left - right) }
}
