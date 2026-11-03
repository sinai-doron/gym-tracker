package com.doron.gymtracker.ui.components

import android.content.res.AssetManager
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.data.catalogue.BodyMap
import com.doron.gymtracker.data.catalogue.BodyRegions
import com.doron.gymtracker.ui.theme.Purple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The body from the front and the back, its chosen muscles filled in. Tapping a muscle hands its
 * muscles to [onRegion] (the front shoulder is front and side delts). Screen readers skip it: the
 * muscle chips beside it say the same.
 */
@Composable
fun BodyMapFigures(
    selected: Set<String>,
    onRegion: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 200.dp
) {
    val assets = LocalContext.current.assets
    val views by produceState<List<DrawnView>?>(null) {
        value = withContext(Dispatchers.Default) { drawnViews(assets) }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally)
    ) {
        views?.forEach { view ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Figure(view, selected, onRegion, Modifier.height(height).aspectRatio(0.5f))
                Text(
                    stringResource(if (view.name == BodyMap.FRONT) R.string.body_front else R.string.body_back),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun Figure(view: DrawnView, selected: Set<String>, onRegion: (List<String>) -> Unit, modifier: Modifier) {
    val onTap by rememberUpdatedState(onRegion)
    val chosen = BodyRegions.highlighted(selected, view.name)
    val muscle = Purple.copy(alpha = 0.16f)
    val other = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val line = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Canvas(
        modifier.pointerInput(view) {
            detectTapGestures { offset ->
                val s = size.width / view.width
                view.partAt(view.left + offset.x / s, view.top + offset.y / s, reach = TAP_REACH.toPx() / s)
                    ?.let { onTap(it.muscles) }
            }
        }
    ) {
        val s = size.width / view.width
        scale(s, s, pivot = Offset.Zero) {
            translate(-view.left, -view.top) {
                view.parts.forEach { part ->
                    val color = when {
                        part.slug in chosen -> Purple
                        part.muscles.isNotEmpty() -> muscle
                        else -> other
                    }
                    drawPath(part.path, color)
                }
                drawPath(view.outline, line, style = Stroke(width = 1.dp.toPx() / s))
            }
        }
    }
}

/** A figure ready to draw: paths parsed once, and each muscle region's area for finding a tap. */
private class DrawnView(val name: String, box: List<Float>, val outline: Path, val parts: List<DrawnPart>) {
    val left = box[0]
    val top = box[1]
    val width = box[2]

    /**
     * The muscle at (x, y), or else the nearest one within [reach]: the figure is small and its
     * muscles have thin gaps between them, which a finger should not have to miss.
     */
    fun partAt(x: Float, y: Float, reach: Float): DrawnPart? {
        for (r in listOf(0f, reach / 2, reach)) {
            val steps = if (r == 0f) 1 else 12
            for (i in 0 until steps) {
                val a = 2 * PI * i / steps
                val px = (x + r * cos(a)).roundToInt()
                val py = (y + r * sin(a)).roundToInt()
                parts.firstOrNull { it.area?.contains(px, py) == true }?.let { return it }
            }
        }
        return null
    }
}

/** How far from a muscle a tap still picks it. */
private val TAP_REACH = 12.dp

private class DrawnPart(val slug: String, val path: Path, val muscles: List<String>, val area: Region?)

private fun drawnViews(assets: AssetManager): List<DrawnView> {
    val map = BodyMap.parse(assets.open(BodyMap.ASSET).bufferedReader().use { it.readText() })
    return listOf(BodyMap.FRONT, BodyMap.BACK).map { name ->
        val view = map.views.getValue(name)
        val bounds = Region(-4000, -4000, 4000, 4000)
        DrawnView(
            name = name,
            box = view.viewBox,
            outline = path(view.outline),
            parts = view.parts.map { part ->
                val path = Path().apply { part.paths.forEach { addPath(path(it)) } }
                val muscles = BodyRegions.muscles(part.slug, name)
                DrawnPart(
                    slug = part.slug,
                    path = path,
                    muscles = muscles,
                    area = if (muscles.isEmpty()) null else Region().apply { setPath(path.asAndroidPath(), bounds) }
                )
            }
        )
    }
}

private fun path(d: String): Path = PathParser().parsePathString(d).toPath()
