package com.doron.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * The end-of-run design's values (the run summary, also a run opened from History), as its
 * design file gives them. The accent is the home screen's.
 */
object RunSummaryColors {
    val ground = Color(0xFFF4F2FB)
    val surface = Color(0xFFFFFFFF)
    val text = Color(0xFF1B1838)
    /** 6.6:1 on white. */
    val textMuted = Color(0xFF5E5A78)
    val accent = AppColors.accentStrength
    /** The gaps between the details' cells, and the bottom bar's top edge. */
    val divider = Color(0xFFE9E6F5)
    /** Lines inside a card: under the distance, between lap rows. */
    val rule = Color(0xFFECE9F5)
    /** Behind the map while its tiles load. */
    val mapGround = Color(0xFFECEAF7)
    val secondaryButton = Color(0xFFECE9FB)
    val secondaryButtonText = Color(0xFF4636C2)
}

object RunSummaryType {
    // Every figure in tabular numerals, so digits line up from row to row
    private val base = AppType.caption.copy(color = RunSummaryColors.text, fontFeatureSettings = "tnum")

    val title = base.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.2.em)
    val subtitle = base.copy(fontSize = 13.sp, color = RunSummaryColors.textMuted)
    val chip = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
    val label = base.copy(fontSize = 13.sp, color = RunSummaryColors.textMuted)
    val distance = base.copy(fontSize = 52.sp, fontWeight = FontWeight.Bold, lineHeight = 1.em, letterSpacing = (-1).sp, color = RunSummaryColors.accent)
    val distanceUnit = base.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = RunSummaryColors.accent)
    val primaryValue = base.copy(fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    val primaryUnit = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = RunSummaryColors.textMuted)
    val cellValue = base.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val cellUnit = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = RunSummaryColors.textMuted)
    val cellLabel = base.copy(fontSize = 12.sp, color = RunSummaryColors.textMuted)
    val sectionTitle = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val sectionNote = base.copy(fontSize = 13.sp, color = RunSummaryColors.textMuted)
    val tableHeader = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = RunSummaryColors.textMuted)
    val tableCell = base.copy(fontSize = 14.sp)
    val tableNote = base.copy(fontSize = 11.sp)
    val button = base.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
}

object RunSummaryShapes {
    val sidePadding = 16.dp
    val sectionGap = 12.dp
    val cardRadius = 20.dp
    val tapTarget = 44.dp

    val backIcon = 22.dp
    val headerPaddingTop = 20.dp
    val headerPaddingBottom = 8.dp
    val headerPaddingStart = 4.dp
    val headerPaddingEnd = 12.dp
    val headerGap = 4.dp
    val contentPaddingTop = 4.dp

    val mapHeight = 210.dp
    val mapRouteWidth = 4f
    val mapOverlayInset = 12.dp
    val mapButtonInset = 8.dp
    val mapButton = 32.dp
    val mapButtonIcon = 16.dp
    val chipPaddingHorizontal = 10.dp
    val chipPaddingVertical = 6.dp
    val chipIcon = 16.dp
    val chipIconGap = 6.dp

    val summaryPaddingHorizontal = 20.dp
    val summaryPaddingTop = 18.dp
    val summaryPaddingBottom = 16.dp
    val summaryGap = 14.dp
    val distanceUnitGap = 6.dp
    val labelValueGap = 2.dp
    val summaryColumnGap = 16.dp

    val cellPaddingHorizontal = 14.dp
    val cellPaddingTop = 14.dp
    val cellPaddingBottom = 12.dp
    val cellGap = 3.dp
    val divider = 1.dp

    val sectionPadding = 16.dp
    val sectionPaddingBottom = 12.dp
    val sectionContentGap = 10.dp
    val lapRowHeight = 44.dp
    val partialLapRowHeight = 52.dp
    val lapCellPadding = 6.dp
    val fastestIcon = 12.dp
    val fastestIconGap = 4.dp
    val trackRowGap = 4.dp

    val barPaddingVertical = 12.dp
    val barPaddingHorizontal = 16.dp
    val buttonHeight = 52.dp
    val buttonRadius = 16.dp
    val buttonIcon = 18.dp
    val buttonIconGap = 8.dp
    val buttonGap = 10.dp
    const val secondaryWeight = 1f
    const val primaryWeight = 1.3f
}
