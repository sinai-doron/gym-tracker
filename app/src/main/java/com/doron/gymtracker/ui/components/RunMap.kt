package com.doron.gymtracker.ui.components

import android.content.Context
import android.view.MotionEvent
import android.view.Gravity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.doron.gymtracker.data.Elevation
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.RouteFollower
import com.doron.gymtracker.data.Slope
import com.doron.gymtracker.domain.model.RoutePoint
import com.doron.gymtracker.ui.theme.Purple
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.sin

/**
 * Map of a run in the app's own style (assets/map_style_run.json, OpenFreeMap vector
 * tiles): the route so far, a planned route to follow, and/or a "you are here" dot with a
 * ring as wide as the fix's accuracy.
 *
 * @param follow keep the map centered on [position] (live); otherwise fit [route] and [plannedRoute]
 */
@Composable
fun RunMap(
    modifier: Modifier = Modifier,
    position: LocationPoint? = null,
    route: List<LocationPoint> = emptyList(),
    /** A route to follow, drawn under the one run so far. */
    plannedRoute: List<RoutePoint> = emptyList(),
    dotColor: Color = Purple,
    follow: Boolean = true,
    interactive: Boolean = false,
    /** Color [plannedRoute] by slope, where it has altitudes; see [SlopeColors]. */
    slopeColors: Boolean = false,
    /** Mark where [plannedRoute] starts and finishes. */
    showEnds: Boolean = false
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        // Texture mode, so the map respects the rounded corners it's clipped to
        val options = MapLibreMapOptions.createFromAttributes(context).textureMode(true)
        if (interactive) {
            MapView(context, options)
        } else {
            // A picture of the route: let touches through, so a page it sits in still scrolls
            object : MapView(context, options) {
                override fun dispatchTouchEvent(event: MotionEvent?): Boolean = false
            }
        }.apply { onCreate(null) }
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    val fitPaddingPx = with(LocalDensity.current) { FIT_PADDING_DP.dp.toPx() }.toInt()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { m ->
            m.uiSettings.apply {
                isLogoEnabled = false
                isCompassEnabled = false
                isAttributionEnabled = true
                attributionGravity = Gravity.BOTTOM or Gravity.START
                setAttributionTintColor(Purple.toArgb())
                setAllGesturesEnabled(interactive)
            }
            // A short or stationary route would otherwise zoom in far past street level
            m.setMaxZoomPreference(MAX_ZOOM)
            m.setStyle(Style.Builder().fromJson(loadStyle(context))) { s ->
                addRunLayers(s)
                map = m
                style = s
            }
        }
    }

    // Draw the latest route and position whenever they (or the style) change
    // Fit again when a different planned route is shown
    val fitted = remember(plannedRoute) { booleanArrayOf(false) }
    val centered = remember { booleanArrayOf(false) }
    LaunchedEffect(style, position, route, plannedRoute, dotColor) {
        val s = style ?: return@LaunchedEffect
        val m = map ?: return@LaunchedEffect
        s.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(
            features(if (route.size >= 2) Feature.fromGeometry(LineString.fromLngLats(route.map { it.toPoint() })) else null)
        )
        s.getSourceAs<GeoJsonSource>(PLANNED_SOURCE)?.setGeoJson(
            features(
                if (plannedRoute.size >= 2) {
                    Feature.fromGeometry(LineString.fromLngLats(plannedRoute.map { Point.fromLngLat(it.longitude, it.latitude) }))
                } else null
            )
        )
        val slopes = if (slopeColors) Elevation.slopes(plannedRoute) else emptyList()
        s.getSourceAs<GeoJsonSource>(SLOPES_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(slopes.map { (slope, points) ->
                Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.longitude, it.latitude) }))
                    .apply { addStringProperty("slope", slope.name) }
            })
        )
        s.getSourceAs<GeoJsonSource>(ENDS_SOURCE)?.setGeoJson(
            FeatureCollection.fromFeatures(if (showEnds && plannedRoute.size >= 2) routeEnds(plannedRoute) else emptyList())
        )
        s.getSourceAs<GeoJsonSource>(DOT_SOURCE)?.setGeoJson(
            features(position?.let { Feature.fromGeometry(it.toPoint()) })
        )
        s.getSourceAs<GeoJsonSource>(ACCURACY_SOURCE)?.setGeoJson(
            features(position?.accuracy?.let { Feature.fromGeometry(circle(position, it.toDouble())) })
        )
        s.getLayer(DOT_LAYER)?.setProperties(PropertyFactory.circleColor(dotColor.toArgb()))
        s.getLayer(ACCURACY_FILL_LAYER)?.setProperties(PropertyFactory.fillColor(dotColor.toArgb()))
        s.getLayer(ACCURACY_EDGE_LAYER)?.setProperties(PropertyFactory.lineColor(dotColor.toArgb()))

        if (follow) {
            position?.let {
                val target = LatLng(it.latitude, it.longitude)
                if (!centered[0]) {
                    m.moveCamera(CameraUpdateFactory.newLatLngZoom(target, FOLLOW_ZOOM))
                    centered[0] = true
                } else {
                    m.animateCamera(CameraUpdateFactory.newLatLng(target), CAMERA_ANIMATION_MS)
                }
            }
        } else if ((route.size >= 2 || plannedRoute.size >= 2) && !fitted[0]) {
            val all = route.map { LatLng(it.latitude, it.longitude) } + plannedRoute.map { LatLng(it.latitude, it.longitude) }
            val bounds = LatLngBounds.Builder().includes(all).build()
            // Wait for layout, or there's nothing to fit the route into
            mapView.post { m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, fitPaddingPx)) }
            fitted[0] = true
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** The colors a slope-colored route is drawn in, for a legend next to the map. */
object SlopeColors {
    val up = Color(0xFFF08C00)
    val down = Color(0xFF12B886)
    val flat = Purple
    val start = Color(0xFF2F9E44)
    val finish = Color(0xFF343A40)
}

/** Start and finish dots; a loop that ends where it began gets just the start. */
private fun routeEnds(route: List<RoutePoint>): List<Feature> {
    fun dot(p: RoutePoint, end: String) =
        Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)).apply { addStringProperty("end", end) }
    val loop = RouteFollower.meters(route.first(), route.last()) < LOOP_M
    return listOfNotNull(if (loop) null else dot(route.last(), "finish"), dot(route.first(), "start"))
}

private const val LOOP_M = 50.0
private const val SLOPES_SOURCE = "run-slopes"
private const val ENDS_SOURCE = "run-ends"
private const val ROUTE_SOURCE = "run-route"
private const val PLANNED_SOURCE = "run-planned"
private const val DOT_SOURCE = "run-position"
private const val ACCURACY_SOURCE = "run-accuracy"
private const val DOT_LAYER = "run-position-dot"
private const val ACCURACY_FILL_LAYER = "run-accuracy-fill"
private const val ACCURACY_EDGE_LAYER = "run-accuracy-edge"
private const val FOLLOW_ZOOM = 16.5
private const val MAX_ZOOM = 17.5
private const val FIT_PADDING_DP = 40
private const val CAMERA_ANIMATION_MS = 600

private fun loadStyle(context: Context): String =
    context.assets.open("map_style_run.json").bufferedReader().use { it.readText() }

private fun features(feature: Feature?): FeatureCollection =
    FeatureCollection.fromFeatures(listOfNotNull(feature))

private fun LocationPoint.toPoint(): Point = Point.fromLngLat(longitude, latitude)

/** Adds the route line, accuracy ring and position dot on top of the base map. */
private fun addRunLayers(style: Style) {
    style.addSource(GeoJsonSource(PLANNED_SOURCE))
    style.addSource(GeoJsonSource(SLOPES_SOURCE))
    style.addSource(GeoJsonSource(ENDS_SOURCE))
    style.addSource(GeoJsonSource(ROUTE_SOURCE))
    style.addSource(GeoJsonSource(ACCURACY_SOURCE))
    style.addSource(GeoJsonSource(DOT_SOURCE))

    // Zoom-based widths must be a top-level interpolate, so the casing gets its own
    fun widthByZoom(atZoom12: Float, atZoom18: Float) = Expression.interpolate(
        Expression.linear(), Expression.zoom(),
        Expression.stop(12, atZoom12), Expression.stop(18, atZoom18)
    )
    // The planned route: soft and wide, so the run drawn over it stands out
    style.addLayer(
        LineLayer("run-planned", PLANNED_SOURCE).withProperties(
            PropertyFactory.lineColor(PLANNED_COLOR),
            PropertyFactory.lineWidth(widthByZoom(5f, 12f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    // The planned route again, by slope, over the plain one (drawn only when asked for)
    style.addLayer(
        LineLayer("run-slopes", SLOPES_SOURCE).withProperties(
            PropertyFactory.lineColor(
                Expression.match(
                    Expression.get("slope"), Expression.color(SlopeColors.flat.toArgb()),
                    Expression.stop(Slope.UP.name, Expression.color(SlopeColors.up.toArgb())),
                    Expression.stop(Slope.DOWN.name, Expression.color(SlopeColors.down.toArgb()))
                )
            ),
            PropertyFactory.lineWidth(widthByZoom(4f, 9f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayer(
        CircleLayer("run-ends", ENDS_SOURCE).withProperties(
            PropertyFactory.circleColor(
                Expression.match(
                    Expression.get("end"), Expression.color(SlopeColors.start.toArgb()),
                    Expression.stop("finish", Expression.color(SlopeColors.finish.toArgb()))
                )
            ),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeWidth(3f)
        )
    )
    style.addLayer(
        LineLayer("run-route-casing", ROUTE_SOURCE).withProperties(
            PropertyFactory.lineColor(android.graphics.Color.WHITE),
            PropertyFactory.lineWidth(widthByZoom(7f, 12f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayer(
        LineLayer("run-route", ROUTE_SOURCE).withProperties(
            PropertyFactory.lineColor(Purple.toArgb()),
            PropertyFactory.lineWidth(widthByZoom(3f, 8f)),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
    )
    style.addLayer(
        FillLayer(ACCURACY_FILL_LAYER, ACCURACY_SOURCE).withProperties(
            PropertyFactory.fillColor(Purple.toArgb()),
            PropertyFactory.fillOpacity(0.15f)
        )
    )
    style.addLayer(
        LineLayer(ACCURACY_EDGE_LAYER, ACCURACY_SOURCE).withProperties(
            PropertyFactory.lineColor(Purple.toArgb()),
            PropertyFactory.lineOpacity(0.45f),
            PropertyFactory.lineWidth(1.5f)
        )
    )
    style.addLayer(
        CircleLayer(DOT_LAYER, DOT_SOURCE).withProperties(
            PropertyFactory.circleColor(Purple.toArgb()),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
            PropertyFactory.circleStrokeWidth(3f)
        )
    )
}

/** A circle [radiusM] meters around [center], as a polygon (MapLibre circles are sized in pixels). */
private fun circle(center: LocationPoint, radiusM: Double, steps: Int = 48): Polygon {
    val latRadius = radiusM / METERS_PER_DEGREE
    val lngRadius = latRadius / cos(Math.toRadians(center.latitude))
    val ring = (0..steps).map { i ->
        val angle = 2 * Math.PI * i / steps
        Point.fromLngLat(center.longitude + lngRadius * cos(angle), center.latitude + latRadius * sin(angle))
    }
    return Polygon.fromLngLats(listOf(ring))
}

private const val METERS_PER_DEGREE = 111_320.0
private val PLANNED_COLOR = android.graphics.Color.parseColor("#B8AEEA")
