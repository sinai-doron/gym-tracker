package com.doron.gymtracker.ui.screens.cardio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.doron.gymtracker.data.Gpx
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.Pace
import com.doron.gymtracker.data.PacePoint
import com.doron.gymtracker.data.RouteDistance
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.ui.components.Activity
import com.doron.gymtracker.ui.components.ActivityIcon
import com.doron.gymtracker.ui.components.valueText
import com.doron.gymtracker.ui.theme.Purple
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** "📤 שתף": the run as an image card (WhatsApp, Instagram…) or as a GPX file (Strava, Garmin…). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareRunButton(state: CardioUiState, records: List<PersonalRecord>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var choosing by remember { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { choosing = true },
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    ) {
        Text("📤 שתף", color = Purple)
    }

    if (choosing) {
        ModalBottomSheet(onDismissRequest = { choosing = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "שיתוף ה${runWord(state.type)}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { choosing = false; previewing = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("🖼 שתף תמונה") }
                OutlinedButton(
                    onClick = { choosing = false; shareGpx(context, state) },
                    enabled = state.route.size >= 2,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("📄 קובץ GPX", color = Purple) }
            }
        }
    }

    if (previewing) ShareImageDialog(state, records, onDismiss = { previewing = false })
}

/** What the shared picture can show; chosen on the share screen and remembered. */
private enum class ShareField(val label: String) {
    DISTANCE("מרחק"), TIME("זמן"), PACE("קצב"), CLIMB("עלייה"), CALORIES("קלוריות"), STEPS("צעדים"),
    DATE("תאריך"), RECORDS("שיאים"), PACE_COLORS("צבעי קצב");

    companion object {
        val DEFAULT = setOf(DISTANCE, DATE, PACE_COLORS)
    }
}

/** The picture as it'll be shared, what to show on it, and the share button. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareImageDialog(state: CardioUiState, records: List<PersonalRecord>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val prefs = remember { context.getSharedPreferences("share_card", Context.MODE_PRIVATE) }
    var fields by remember {
        mutableStateOf(
            prefs.getStringSet("fields", null)
                ?.mapNotNull { name -> ShareField.entries.firstOrNull { it.name == name } }?.toSet()
                ?: ShareField.DEFAULT
        )
    }
    // Only what this run has can be chosen
    val available = ShareField.entries.filter {
        when (it) {
            ShareField.CALORIES -> state.calories != null
            ShareField.STEPS -> state.steps != null
            ShareField.RECORDS -> records.isNotEmpty()
            ShareField.PACE_COLORS -> state.route.size >= 2
            ShareField.PACE -> state.avgPaceSecsPerKm != null
            else -> true
        }
    }
    val paceColors = ShareField.PACE_COLORS in fields
    // Null while the map is drawn; drawn again when the route's coloring is switched
    val map by produceState<Bitmap?>(initialValue = null, state.route, paceColors) {
        value = null
        value = if (state.route.size >= 2) routeSnapshot(context, state, paceColors) else null
    }
    val ready = state.route.size < 2 || map != null

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(CARD_SIZE)
                    // Drawn into a layer, so the very same pixels can be saved as the image
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
            ) {
                RunShareCard(state, records, map, fields)
                if (!ready) CircularProgressIndicator(color = Purple, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                modifier = Modifier.width(CARD_SIZE)
            ) {
                available.forEach { field ->
                    val on = field in fields
                    FilterChip(
                        selected = on,
                        onClick = {
                            fields = if (on) fields - field else fields + field
                            prefs.edit().putStringSet("fields", fields.map { it.name }.toSet()).apply()
                        },
                        label = { Text(field.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            selectedContainerColor = Purple,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, shape = MaterialTheme.shapes.large) {
                    Text("ביטול", color = Color.White)
                }
                Button(
                    onClick = {
                        scope.launch {
                            val bitmap = layer.toImageBitmap().asAndroidBitmap()
                            val file = withContext(Dispatchers.IO) {
                                sharedFile(context, "run_${state.startedAt ?: 0}.png").also { f ->
                                    f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                }
                            }
                            share(context, file, "image/png")
                            onDismiss()
                        }
                    },
                    enabled = ready,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple),
                    shape = MaterialTheme.shapes.large
                ) { Text("📤 שתף תמונה") }
            }
        }
    }
}

/**
 * The shared picture: the route on a full-bleed map, the activity and date along the top,
 * and the chosen numbers at the bottom, the first of them large.
 */
@Composable
private fun RunShareCard(state: CardioUiState, records: List<PersonalRecord>, map: Bitmap?, fields: Set<ShareField>) {
    val numbers = listOfNotNull(
        (ShareField.DISTANCE in fields).takeIf { it }?.let { "%.2f".format(state.distanceMeters / 1000) to "ק״מ" },
        (ShareField.TIME in fields).takeIf { it }?.let { formatClock(state.elapsedSecs) to "זמן" },
        state.avgPaceSecsPerKm?.takeIf { ShareField.PACE in fields }?.let { formatClock(it) to "דק׳/ק״מ" },
        (ShareField.CLIMB in fields).takeIf { it }?.let { "${state.elevationGainM.roundToInt()}" to "מ׳ עלייה" },
        state.calories?.takeIf { ShareField.CALORIES in fields }?.let { "$it" to "קק״ל" },
        state.steps?.takeIf { ShareField.STEPS in fields }?.let { "%,d".format(it) to "צעדים" }
    )
    val ink = Color(0xFF1A1035)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF0EEFF))
    ) {
        map?.let {
            Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        // Soft fades, so the text reads over any map
        Box(
            Modifier.fillMaxWidth().height(72.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f), Color.Transparent)))
        )
        Box(
            Modifier.fillMaxWidth().height(if (numbers.size > 1 || ShareField.RECORDS in fields) 150.dp else 110.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.94f))))
        )
        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActivityIcon(if (state.type == "WALKING") Activity.WALK else Activity.RUN, size = 30.dp)
            Spacer(Modifier.weight(1f))
            if (ShareField.DATE in fields) {
                state.startedAt?.let {
                    Text(
                        SimpleDateFormat("d בMMM · HH:mm", Locale.forLanguageTag("he")).format(Date(it)),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ink
                    )
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (ShareField.RECORDS in fields && records.isNotEmpty()) {
                Text(
                    "🏆 " + records.joinToString(" · ") { "${it.kind.label} ${it.valueText()}" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
            }
            if (numbers.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    numbers.drop(1).forEach { (value, unit) ->
                        Column {
                            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = ink)
                            Text(unit, fontSize = 11.sp, color = Color(0xFF6B6280))
                        }
                    }
                }
            }
            numbers.firstOrNull()?.let { (value, unit) ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = ink)
                    Spacer(Modifier.width(6.dp))
                    Text(unit, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}

/** The run's route drawn on the app's map style, framed to fit, as a square picture. */
private suspend fun routeSnapshot(context: Context, state: CardioUiState, paceColors: Boolean): Bitmap? {
    val route = state.route
    val gradient = if (paceColors) state.clock?.let { paceGradient(route, Pace.profile(route, it, state.pauses)) } else null
    return suspendCancellableCoroutine { cont ->
        MapLibre.getInstance(context)
        val options = MapSnapshotter.Options(SNAPSHOT_SIZE, SNAPSHOT_SIZE)
            .withStyleBuilder(Style.Builder().fromJson(styleWithRoute(context, route, gradient)))
            .withRegion(framed(route))
            .withPixelRatio(context.resources.displayMetrics.density)
            .withLogo(false)
        val snapshotter = MapSnapshotter(context, options)
        snapshotter.start(
            { snapshot -> if (cont.isActive) cont.resume(snapshot.bitmap) },
            { error ->
                android.util.Log.w("RunShare", "Map snapshot failed: $error")
                if (cont.isActive) cont.resume(null)
            }
        )
        cont.invokeOnCancellation { snapshotter.cancel() }
    }
}

/**
 * A line-gradient expression coloring the route by pace: blue where fastest, through green
 * and yellow, to red where slowest. Pace is placed by distance along the drawn line; the
 * ends of the scale are the run's 10th and 90th percentile, so a stop or a sprint doesn't
 * wash the rest out. Null when the run is too short for a pace.
 */
private fun paceGradient(route: List<LocationPoint>, rawPace: List<PacePoint>): JSONArray? {
    if (rawPace.size < 2) return null
    // Averaged over a few hundred meters, so the line reads as stretches rather than flicker
    val pace = rawPace.indices.map { i ->
        val near = rawPace.subList((i - SMOOTH_POINTS).coerceAtLeast(0), (i + SMOOTH_POINTS + 1).coerceAtMost(rawPace.size))
        rawPace[i].copy(secsPerKm = near.map { it.secsPerKm }.average())
    }
    val along = route.runningFold(0.0 to route.first()) { (m, prev), p -> (m + RouteDistance.metersBetween(prev, p)) to p }
        .drop(1).map { it.first }
    val total = along.last().takeIf { it > 0 } ?: return null
    // Pace is measured along the filtered distance, a little shorter than the drawn line
    val scale = pace.last().meters / total
    val sorted = pace.map { it.secsPerKm }.sorted()
    val fast = sorted[sorted.size / 10]
    val slow = sorted[sorted.size * 9 / 10].coerceAtLeast(fast + 1)
    val expression = JSONArray().put("interpolate").put(JSONArray().put("linear")).put(JSONArray().put("line-progress"))
    var lastStop = -1.0
    for (i in route.indices) {
        val progress = along[i] / total
        if (progress - lastStop < 0.02 && i != route.lastIndex) continue
        if (progress <= lastStop) continue
        val secs = pace.minBy { kotlin.math.abs(it.meters - along[i] * scale) }.secsPerKm
        expression.put(progress).put(paceColor(((secs - fast) / (slow - fast)).coerceIn(0.0, 1.0)))
        lastStop = progress
    }
    return expression
}

private val PACE_SCALE = listOf(0xFF2F6FED, 0xFF22B573, 0xFFF2C230, 0xFFE8453C).map { Color(it) }

/** 0 = fastest (blue) to 1 = slowest (red), as "#RRGGBB". */
private fun paceColor(t: Double): String {
    val pos = t * (PACE_SCALE.size - 1)
    val i = pos.toInt().coerceAtMost(PACE_SCALE.size - 2)
    val c = lerp(PACE_SCALE[i], PACE_SCALE[i + 1], (pos - i).toFloat())
    return "#%06X".format(c.toArgb() and 0xFFFFFF)
}

/**
 * The run map's style with the route and its ends written into it, plain purple or colored
 * by [gradient]. Written in rather than added through the snapshotter, which crashes adding
 * sources to a style given as text.
 */
private fun styleWithRoute(context: Context, route: List<LocationPoint>, gradient: JSONArray?): String {
    val style = JSONObject(context.assets.open("map_style_run.json").bufferedReader().use { it.readText() })
    val line = Feature.fromGeometry(LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) }))
    fun dot(p: LocationPoint, end: String) =
        Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)).apply { addStringProperty("end", end) }
    val ends = FeatureCollection.fromFeatures(listOf(dot(route.last(), "finish"), dot(route.first(), "start")))
    val purple = "#%06X".format(Purple.toArgb() and 0xFFFFFF)
    style.getJSONObject("sources")
        // Line metrics give the gradient its position along the line
        .put("share-route", JSONObject().put("type", "geojson").put("lineMetrics", true).put("data", JSONObject(line.toJson())))
        .put("share-ends", JSONObject().put("type", "geojson").put("data", JSONObject(ends.toJson())))
    fun lineLayer(id: String, width: Double, paint: JSONObject) = JSONObject()
        .put("id", id).put("type", "line").put("source", "share-route")
        .put("layout", JSONObject().put("line-cap", "round").put("line-join", "round"))
        .put("paint", paint.put("line-width", width))
    style.getJSONArray("layers")
        .put(lineLayer("share-route-casing", 9.0, JSONObject().put("line-color", "#FFFFFF")))
        .put(
            lineLayer(
                "share-route-line", 5.0,
                if (gradient != null) JSONObject().put("line-gradient", gradient) else JSONObject().put("line-color", purple)
            )
        )
        .put(
            JSONObject().put("id", "share-ends-dots").put("type", "circle").put("source", "share-ends")
                .put(
                    "paint", JSONObject()
                        .put("circle-color", JSONArray("""["match", ["get", "end"], "start", "#22B573", "#1A1035"]"""))
                        .put("circle-radius", 6)
                        .put("circle-stroke-color", "#FFFFFF").put("circle-stroke-width", 2.5)
                )
        )
    return style.toString()
}

/** The route's bounds with room around it (more below, under the numbers), never closer than a few streets. */
private fun framed(route: List<LocationPoint>): LatLngBounds {
    val south = route.minOf { it.latitude }
    val north = route.maxOf { it.latitude }
    val west = route.minOf { it.longitude }
    val east = route.maxOf { it.longitude }
    val latSpan = maxOf(north - south, MIN_SPAN_DEG)
    val lonSpan = maxOf(east - west, MIN_SPAN_DEG)
    return LatLngBounds.Builder()
        .include(LatLng(south - latSpan * 0.35, west - lonSpan * 0.12))
        .include(LatLng(north + latSpan * 0.18, east + lonSpan * 0.12))
        .build()
}

private fun shareGpx(context: Context, state: CardioUiState) {
    val start = state.startedAt ?: return
    val name = "${runWord(state.type)} " + SimpleDateFormat("d.M.yyyy", Locale.getDefault()).format(Date(start))
    val file = sharedFile(context, "run_$start.gpx")
    file.writeText(Gpx.write(name, state.type, state.route))
    share(context, file, "application/gpx+xml")
}

/** A file in the cache folder the app shares from (see res/xml/file_paths.xml). */
private fun sharedFile(context: Context, name: String): File =
    File(context.cacheDir, "shared").apply { mkdirs() }.resolve(name)

private fun share(context: Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null))
}

private fun runWord(type: String) = if (type == "WALKING") "הליכה" else "ריצה"

private fun formatClock(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private val CARD_SIZE = 340.dp
// Square, the card's size in dp, drawn at the screen's density
private const val SNAPSHOT_SIZE = 340
private const val MIN_SPAN_DEG = 0.004
// Pace points either side averaged for the route's colors: ±3 × 50 m
private const val SMOOTH_POINTS = 3
