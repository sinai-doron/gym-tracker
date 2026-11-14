package com.doron.gymtracker.ui.components

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.doron.gymtracker.data.places.Place
import kotlin.math.asin

/**
 * The ribbon's centre line, as a share of the badge's width: an arc through the two stars and
 * the middle of the band (measured on the 1024 px art, scripts/make_place_badges.py).
 */
private object Ribbon {
    const val TOP_X = 516f / 1024
    const val TOP_Y = 731f / 1024
    const val SAG = 16f / 1024
    const val CHORD = 408f / 1024
    // As large as the band takes (it is 80 of 1024 tall in the middle), up to just short of the stars:
    // a badge is shown about 100 dp wide, where smaller letters can't be read
    const val MAX_TEXT = 370f / 1024
    const val TEXT_SIZE = 64f / 1024
    const val MIN_TEXT_SIZE = 24f / 1024
    val RADIUS = CHORD * CHORD / (8 * SAG) + SAG / 2
}

/** A place's badge, with its name on the ribbon in the app's language; greyed out until earned. */
@SuppressLint("DiscouragedApi") // Looked up by name; res/raw/keep_place_badges.xml keeps them
@Composable
fun PlaceBadgeImage(place: Place, earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable = remember(place.id) {
        context.resources.getIdentifier("badge_place_${place.id.replace('-', '_')}", "drawable", context.packageName)
    }
    if (drawable == 0) return
    val locale = appLocale()
    val name = place.nameIn(appLanguage()).let { if (appLanguage() == "en") it.uppercase(locale) else it }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .alpha(if (earned) 1f else 0.45f)
            .semantics { contentDescription = name }
    ) {
        Image(
            painter = painterResource(drawable),
            contentDescription = null,
            colorFilter = if (earned) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
            modifier = Modifier.matchParentSize()
        )
        Canvas(Modifier.matchParentSize()) {
            val s = this.size.width
            val r = Ribbon.RADIUS * s
            val cx = Ribbon.TOP_X * s
            val cy = Ribbon.TOP_Y * s + r
            // From the left star to the right one, along the circle's top
            val half = Math.toDegrees(asin((Ribbon.CHORD * s / 2 / r).toDouble())).toFloat()
            val arc = android.graphics.Path().apply { addArc(RectF(cx - r, cy - r, cx + r, cy + r), 270f - half, 2 * half) }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                color = if (earned) android.graphics.Color.WHITE else android.graphics.Color.rgb(236, 236, 240)
                textSize = Ribbon.TEXT_SIZE * s
                setShadowLayer(0.004f * s, 0.002f * s, 0.003f * s, android.graphics.Color.argb(170, 30, 10, 70))
            }
            // Long names shrink to fit between the stars
            while (paint.measureText(name) > Ribbon.MAX_TEXT * s && paint.textSize > Ribbon.MIN_TEXT_SIZE * s) {
                paint.textSize -= 0.002f * s
            }
            val bounds = android.graphics.Rect().also { paint.getTextBounds(name, 0, name.length, it) }
            val length = PathMeasure(arc, false).length
            drawContext.canvas.nativeCanvas.drawTextOnPath(
                name, arc,
                (length - paint.measureText(name)) / 2,
                // Letters centred on the line: half their height below it, to the baseline
                -(bounds.top + bounds.bottom) / 2f,
                paint
            )
        }
    }
}
