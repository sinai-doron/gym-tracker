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
import com.doron.gymtracker.data.PacePoint
import com.doron.gymtracker.ui.theme.Purple
import kotlin.math.roundToInt

/**
 * Altitude along the route, distance on the x axis (left to right, also in RTL). Touch or
 * drag to read the distance and altitude at a point; with nothing touched it shows the
 * lowest and highest points.
 */
@Composable
fun ElevationChart(points: List<ElevationPoint>, modifier: Modifier = Modifier) =
    DistanceChart(
        points = points.map { it.meters to it.altitudeM },
        format = { "${it.roundToInt()} מ׳" },
        // The chart shows at least this much height, so a 2 m wobble isn't drawn as a cliff
        minRange = 20.0,
        modifier = modifier
    )

/** Pace along the run, faster higher up; touch or drag to read it at a point. */
@Composable
fun PaceChart(points: List<PacePoint>, modifier: Modifier = Modifier) =
    DistanceChart(
        points = points.map { it.meters to it.secsPerKm },
        format = { secs -> "%d:%02d /ק״מ".format(secs.roundToInt() / 60, secs.roundToInt() % 60) },
        // A minute a km at least, so steady running reads as steady
        minRange = 60.0,
        faster = true,
        modifier = modifier
    )

/**
 * A value against distance (left to right, also in RTL), filled below the line. Touch or
 * drag to read the distance and value at a point; with nothing touched the header shows
 * the range. [faster] turns the y axis over, for values where lower is better (pace).
 */
@Composable
private fun DistanceChart(
    points: List<Pair<Double, Double>>,
    format: (Double) -> String,
    minRange: Double,
    modifier: Modifier = Modifier,
    faster: Boolean = false
) {
    var selected by remember(points) { mutableStateOf<Pair<Double, Double>?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val surface = MaterialTheme.colorScheme.surface
    val minValue = points.minOf { it.second }
    val maxValue = points.maxOf { it.second }
    val totalM = points.last().first.coerceAtLeast(1.0)

    fun nearest(x: Float, width: Float, left: Float, right: Float): Pair<Double, Double> {
        val meters = ((x - left) / (width - left - right)).coerceIn(0f, 1f) * totalM
        return points.minBy { kotlin.math.abs(it.first - meters) }
    }

    Column(modifier) {
        Text(
            selected?.let { "%.2f ק״מ · %s".format(it.first / 1000, format(it.second)) }
                // The range isolated left to right, or Hebrew around it shows "50–20"
                ?: if (format(minValue) == format(maxValue)) format(minValue)
                else "\u2066${format(minValue).substringBefore(" ")}–${format(maxValue).substringBefore(" ")}\u2069 " +
                    format(minValue).substringAfter(" "),
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
                // Pad the range so gentle changes don't look like cliffs, and flat ones sit mid-chart
                val pad = maxOf(minRange - (maxValue - minValue), 0.0) / 2 + (maxValue - minValue) * 0.1
                val lo = minValue - pad
                val hi = maxValue + pad
                fun x(m: Double) = (left + m / totalM * (size.width - left - right)).toFloat()
                fun y(v: Double): Float {
                    val f = (v - lo) / (hi - lo)
                    return (bottom - (if (faster) 1 - f else f) * (bottom - top)).toFloat()
                }

                // Recessive grid: the extremes
                listOf(minValue, maxValue).distinct().forEach { v ->
                    drawLine(axisColor, Offset(left, y(v)), Offset(size.width - right, y(v)), 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                    val label = textMeasurer.measure(if (faster) format(v).substringBefore(" ") else format(v), labelStyle)
                    drawText(label, topLeft = Offset(0f, y(v) - label.size.height / 2))
                }
                val start = textMeasurer.measure("0", labelStyle)
                drawText(start, topLeft = Offset(left, bottom + 4.dp.toPx()))
                val end = textMeasurer.measure("%.1f ק״מ".format(totalM / 1000), labelStyle)
                drawText(end, topLeft = Offset(size.width - right - end.size.width, bottom + 4.dp.toPx()))

                val line = Path().apply {
                    points.forEachIndexed { i, (m, v) ->
                        if (i == 0) moveTo(x(m), y(v)) else lineTo(x(m), y(v))
                    }
                }
                val area = Path().apply {
                    addPath(line)
                    lineTo(x(points.last().first), bottom)
                    lineTo(x(points.first().first), bottom)
                    close()
                }
                drawPath(area, Purple.copy(alpha = 0.12f))
                drawPath(line, Purple, style = Stroke(width = 2.dp.toPx()))

                selected?.let { (m, v) ->
                    val c = Offset(x(m), y(v))
                    drawLine(axisColor, Offset(c.x, top), Offset(c.x, bottom), 1.dp.toPx())
                    drawCircle(surface, 8.dp.toPx(), c)
                    drawCircle(Purple, 6.dp.toPx(), c)
                }
            }
        }
    }
}

private val LABEL_COLUMN = 48.dp
