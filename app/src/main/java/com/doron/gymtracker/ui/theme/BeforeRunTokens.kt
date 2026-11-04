package com.doron.gymtracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The before-run design's values (finding GPS, then GPS ready), as its design files give them.
 * Shares the end-of-run screen's palette where the two agree.
 */
object BeforeRunColors {
    val mapGround = RunSummaryColors.mapGround
    val panel = RunSummaryColors.surface
    /** The target row, and the veil over the map while GPS is found. */
    val softSurface = RunSummaryColors.ground
    val text = RunSummaryColors.text
    val textMuted = RunSummaryColors.textMuted
    val accent = RunSummaryColors.accent
    /** Behind the spinner while GPS is found. */
    val accentTint = Color(0xFFECE9FB)
    /** The spinner's track, and signal bars not yet reached. */
    val track = Color(0xFFD6D1F3)
    val success = Color(0xFF0B7A57)
    val successTint = Color(0xFFE1F4EC)
    val disabledButton = Color(0xFFE4E1F2)
    /** 6.9:1 on [disabledButton]: a button that can't be pressed yet still reads. */
    val disabledButtonText = Color(0xFF4F4B69)
    val shadow = Color(0xFF1B1838)
}

object BeforeRunType {
    // Rubik in the text colour, tabular numerals
    private val base = RunSummaryType.title

    val statusTitle = base.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    val statusSubtitle = RunSummaryType.subtitle
    val targetLabel = RunSummaryType.cellLabel
    val targetValue = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium)
    val targetNote = RunSummaryType.cellLabel
    val button = base.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    val sheetTitle = RunSummaryType.title
    val option = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal)
}

object BeforeRunShapes {
    val tapTarget = 44.dp
    val sideInset = 16.dp
    val backTop = 20.dp
    val backIcon = 22.dp
    val mapButtonIcon = 20.dp
    /** Between the re-centre button and the panel. */
    val mapButtonGap = 16.dp
    val buttonShadow = 2.dp

    val panelRadius = 28.dp
    val panelPaddingTop = 22.dp
    val panelPaddingBottom = 34.dp
    val panelGap = 16.dp
    val panelShadow = 12.dp

    val statusIcon = 44.dp
    val statusGap = 12.dp
    val spinner = 24.dp
    val spinnerStroke = 2.5.dp
    val checkIcon = 22.dp
    val titleGap = 2.dp
    val barWidth = 4.dp
    val barGap = 2.dp
    val barRadius = 1.dp
    val barHeights = listOf(6.dp, 10.dp, 14.dp, 18.dp)

    val targetMinHeight = 60.dp
    val targetPaddingVertical = 10.dp
    val targetIcon = 22.dp
    val chevron = 20.dp
    val targetLineGap = 1.dp

    val buttonHeight = 56.dp
    val buttonRadius = 16.dp
    val buttonIcon = 18.dp
    val buttonIconGap = 10.dp
    val buttonSpinnerStroke = 2.dp

    /** The rings that pulse while GPS is found, and the pin in the middle of them. */
    val ringStep = 40.dp
    val ringStrokes = listOf(2.dp, 1.5.dp, 1.dp)
    val ringAlphas = listOf(0.5f, 0.3f, 0.15f)
    val pinCircle = 44.dp
    val pinIcon = 24.dp
    const val veilAlpha = 0.65f

    val sheetOptionHeight = 52.dp
}
