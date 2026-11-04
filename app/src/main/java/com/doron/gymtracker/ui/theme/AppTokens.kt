package com.doron.gymtracker.ui.theme

import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.R

/**
 * The home screen design's values (the next workout card, the run and walk card, the ⋮ menu),
 * as the design files give them: no widget there uses a colour or size of its own.
 */
object AppColors {
    /** Filled buttons, active icons, purple tiles. */
    val accentStrength = Color(0xFF5B4BD6)
    /** Filled buttons and icons on cardio surfaces. */
    val accentCardio = Color(0xFF0E9F75)
    /** Icon tile background, the highlighted menu row. */
    val tintStrength = Color(0xFFEFEDFB)
    /** The cardio icon tile and secondary cardio button. */
    val tintCardio = Color(0xFFE4F6EF)
    val cardioTextOnTint = Color(0xFF0B6B4F)
    /** Screen background, neutral chips, neutral icon tiles. */
    val ground = Color(0xFFF3F2F9)
    val surface = Color(0xFFFFFFFF)
    /** List rows inside cards. */
    val surfaceMuted = Color(0xFFF7F6FC)
    val border = Color(0xFFECEAF6)
    /** Outlined buttons. */
    val borderStrong = Color(0xFFE5E3F1)
    val textPrimary = Color(0xFF1E1D2B)
    val textSecondary = Color(0xFF4B4A5E)
    val textMuted = Color(0xFF6B6A80)
    val chevron = Color(0xFF8B8A9E)
    val warnDot = Color(0xFFF0A33A)
    val warnTint = Color(0xFFFFF4E3)
    val warnText = Color(0xFF8A5A0E)
}

private val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/** Rubik from Google Fonts, fetched by Play services; the system font until it arrives. */
val Rubik = FontFamily(
    listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { weight ->
        Font(googleFont = GoogleFont("Rubik"), fontProvider = fontProvider, weight = weight)
    }
)

object AppType {
    // As CSS lays out a line: the extra height split evenly above and below
    private val cssLines = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)
    private val base = TextStyle(fontFamily = Rubik, lineHeightStyle = cssLines)

    val label = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.04.em, color = AppColors.textMuted)
    val cardTitle = base.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 1.1.em, color = AppColors.textPrimary)
    val headerTitle = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
    val button = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val menuItem = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AppColors.textPrimary)
    val menuItemHighlighted = menuItem.copy(fontWeight = FontWeight.SemiBold)
    val chip = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val caption = base.copy(fontSize = 13.sp, color = AppColors.textMuted)
    val hint = base.copy(fontSize = 12.sp, color = AppColors.textMuted)
}

/** One layer of a CSS box-shadow: offset, blur and colour. */
data class ShadowLayer(val x: Dp, val y: Dp, val blur: Dp, val color: Color)

object AppShapes {
    // Card
    val cardRadius = 20.dp
    val cardPaddingVertical = 20.dp
    val cardPaddingHorizontal = 22.dp
    val cardBorder = 1.dp
    val cardGap = 16.dp
    val cardShadow = listOf(
        ShadowLayer(0.dp, 1.dp, 2.dp, Color(30, 29, 43).copy(alpha = 0.06f)),
        ShadowLayer(0.dp, 8.dp, 24.dp, Color(91, 75, 214).copy(alpha = 0.08f))
    )
    val cardioCardShadow = listOf(
        ShadowLayer(0.dp, 1.dp, 2.dp, Color(30, 29, 43).copy(alpha = 0.06f)),
        ShadowLayer(0.dp, 8.dp, 24.dp, Color(16, 150, 110).copy(alpha = 0.08f))
    )

    // A card's header: tile, then label over title, then an action
    val headerGap = 14.dp
    val labelTitleGap = 3.dp

    // Icon tiles
    val iconTile = 56.dp
    val iconTileRadius = 18.dp
    val iconTileIcon = 28.dp
    val smallIconTile = 32.dp
    val smallIconTileRadius = 10.dp
    val smallIconTileIcon = 18.dp

    // Buttons
    val buttonHeight = 50.dp
    val buttonRadius = 14.dp
    val buttonIcon = 18.dp
    val buttonIconGap = 8.dp
    val buttonRowGap = 10.dp
    val buttonBorder = 1.dp
    /** The 50×50 icon button beside a primary button, and its icon. */
    val sideIconButton = 50.dp
    val sideIconButtonIcon = 20.dp
    /** The card header's icon button, and its icon. */
    val cardIconButton = 40.dp
    val cardIconButtonRadius = 12.dp
    val cardIconButtonIcon = 18.dp
    /** The screen header's icon buttons, and their icons. */
    val headerIconButton = 44.dp
    val headerIconButtonRadius = 12.dp
    val headerIconButtonIcon = 22.dp
    val headerIconButtonGap = 4.dp
    val headerIconButtonPressedShadow = listOf(ShadowLayer(0.dp, 1.dp, 2.dp, Color(30, 29, 43).copy(alpha = 0.06f)))

    // Chips
    val chipHeight = 28.dp
    val chipPaddingHorizontal = 10.dp
    val chipGap = 8.dp
    val chipDot = 6.dp
    val chipDotGap = 6.dp

    // A list row inside a card
    val listRowHeight = 52.dp
    val listRowRadius = 14.dp
    val listRowPaddingHorizontal = 12.dp

    // The menu
    val menuWidth = 268.dp
    val menuRadius = 18.dp
    val menuPadding = 8.dp
    val menuBorder = 1.dp
    val menuShadow = listOf(
        ShadowLayer(0.dp, 2.dp, 4.dp, Color(30, 29, 43).copy(alpha = 0.06f)),
        ShadowLayer(0.dp, 16.dp, 40.dp, Color(30, 29, 43).copy(alpha = 0.14f))
    )
    val menuRowGap = 2.dp
    val menuRowHeight = 44.dp
    val menuActionRowHeight = 48.dp
    val menuRowRadius = 12.dp
    val menuRowPaddingHorizontal = 10.dp
    val menuRowIconGap = 12.dp
    val menuActionTextGap = 1.dp
    val menuDividerVertical = 6.dp
    val menuDividerHorizontal = 10.dp
    /** From the ⋮ button's bottom to the menu's top, and how far past the button's outer edge the menu reaches. */
    val menuOffsetY = 16.dp
    val menuOffsetX = 4.dp
}

/**
 * CSS box-shadow [layers] under a rounded rectangle of [radius]. Compose's elevation shadow can't
 * take a blur, offset and colour, so each layer is a blurred shadow drawn behind (from Android 9;
 * earlier versions draw none).
 */
fun Modifier.designShadow(layers: List<ShadowLayer>, radius: Dp): Modifier = drawBehind {
    drawIntoCanvas { canvas ->
        layers.forEach { layer ->
            val paint = Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.TRANSPARENT
                // CSS blur is twice the Gaussian sigma; Android's radius is (sigma - 0.5) / 0.57735
                val sigma = layer.blur.toPx() / 2f
                setShadowLayer(((sigma - 0.5f) / 0.57735f).coerceAtLeast(0.01f), layer.x.toPx(), layer.y.toPx(), layer.color.toArgb())
            }
            val r = radius.toPx()
            canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
        }
    }
}

