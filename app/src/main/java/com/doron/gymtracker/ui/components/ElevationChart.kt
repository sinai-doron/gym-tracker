package com.doron.gymtracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.doron.gymtracker.data.ElevationPoint
import com.doron.gymtracker.ui.theme.Purple
import kotlin.math.roundToInt

/**
 * Altitude along the route, distance on the x axis (left to right, also in RTL). Touch or
 * drag to read the distance and altitude at a point; with nothing touched it shows the
 * lowest and highest points.
 */
@Composable
fun ElevationChart(points: List<ElevationPoint>, modifier: Modifier = Modifier) {
    var selected by remember(points) { mutableStateOf<ElevationPoint?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val surface = MaterialTheme.colorScheme.surface
    val minAlt = points.minOf { it.altitudeM }
    val maxAlt = points.maxOf { it.altitudeM }
    val totalM = points.last().meters.coerceAtLeast(1.0)

    fun nearest(x: Float, width: Float, left: Float, right: Float): ElevationPoint {
        val meters = ((x - left) / (width - left - right)).coerceIn(0f, 1f) * totalM
        return points.minBy { kotlin.math.abs(it.meters - meters) }
    }

    Column(modifier) {
        Text(
            selected?.let { "%.2f ק״מ · %d מ׳".format(it.meters / 1000, it.altitudeM.roundToInt()) }
                ?: "%d–%d מ׳".format(minAlt.roundToInt(), maxAlt.roundToInt()),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(points) {
                        val left = LABEL_COLUMN.toPx()
                        val right = 8.dp.toPx()
                        detectTapGestures { selected = nearest(it.x, size.width.toFloat(), left, right) }
                    }
                    .pointerInput(points) {
                        val left = LABEL_COLUMN.toPx()
                        val right = 8.dp.toPx()
                        detectDragGestures { change, _ ->
                            selected = nearest(change.position.x, size.width.toFloat(), left, right)
                        }
                    }
            ) {
                val left = LABEL_COLUMN.toPx()
                val right = 8.dp.toPx()
                val top = 8.dp.toPx()
                val bottom = size.height - 20.dp.toPx()
                // Pad the range so gentle routes don't look like mountains, and flat ones sit mid-chart
                val pad = maxOf(MIN_RANGE_M - (maxAlt - minAlt), 0.0) / 2 + (maxAlt - minAlt) * 0.1
                val lo = minAlt - pad
                val hi = maxAlt + pad
                fun x(m: Double) = (left + m / totalM * (size.width - left - right)).toFloat()
                fun y(a: Double) = (bottom - (a - lo) / (hi - lo) * (bottom - top)).toFloat()

                // Recessive grid: lowest and highest altitude
                listOf(minAlt, maxAlt).distinct().forEach { a ->
                    drawLine(axisColor, Offset(left, y(a)), Offset(size.width - right, y(a)), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    val label = textMeasurer.measure("${a.roundToInt()} מ׳", labelStyle)
                    drawText(label, topLeft = Offset(0f, y(a) - label.size.height / 2))
                }
                val start = textMeasurer.measure("0", labelStyle)
                drawText(start, topLeft = Offset(left, bottom + 4.dp.toPx()))
                val end = textMeasurer.measure("%.1f ק״מ".format(totalM / 1000), labelStyle)
                drawText(end, topLeft = Offset(size.width - right - end.size.width, bottom + 4.dp.toPx()))

                val line = Path().apply {
                    points.forEachIndexed { i, p ->
                        if (i == 0) moveTo(x(p.meters), y(p.altitudeM)) else lineTo(x(p.meters), y(p.altitudeM))
                    }
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(x(points.last().meters), bottom)
                    lineTo(x(points.first().meters), bottom)
                    close()
                }
                drawPath(area, Purple.copy(alpha = 0.12f))
                drawPath(line, Purple, style = Stroke(width = 2.dp.toPx()))

                selected?.let { p ->
                    val c = Offset(x(p.meters), y(p.altitudeM))
                    drawLine(axisColor, Offset(c.x, top), Offset(c.x, bottom), 1.dp.toPx())
                    drawCircle(surface, 8.dp.toPx(), c)
                    drawCircle(Purple, 6.dp.toPx(), c)
                }
            }
        }
    }
}

private val LABEL_COLUMN = 48.dp
// The chart shows at least this much height, so a 2 m wobble isn't drawn as a cliff
private const val MIN_RANGE_M = 20.0
