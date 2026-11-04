package com.doron.gymtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import com.doron.gymtracker.ui.theme.AppType
import com.doron.gymtracker.ui.theme.ShadowLayer
import com.doron.gymtracker.ui.theme.designShadow

/** The design's white card: border, two-layer shadow, sections [AppShapes.cardGap] apart. */
@Composable
fun DesignCard(
    modifier: Modifier = Modifier,
    shadow: List<ShadowLayer> = AppShapes.cardShadow,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppShapes.cardRadius)
    Column(
        verticalArrangement = Arrangement.spacedBy(AppShapes.cardGap),
        modifier = modifier
            .fillMaxWidth()
            .designShadow(shadow, AppShapes.cardRadius)
            .background(AppColors.surface, shape)
            .border(AppShapes.cardBorder, AppColors.border, shape)
            // As in CSS, the border takes room of its own: Compose draws it over the padding
            .padding(AppShapes.cardBorder)
            .padding(horizontal = AppShapes.cardPaddingHorizontal, vertical = AppShapes.cardPaddingVertical),
        content = content
    )
}

/** A card's header: the icon tile, the label (if any) over the title, and an optional action at the end. */
@Composable
fun DesignCardHeader(
    @DrawableRes icon: Int,
    tile: Color,
    iconColor: Color,
    label: String?,
    title: String,
    action: (@Composable () -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppShapes.headerGap)) {
        IconTile(icon, tile, iconColor, AppShapes.iconTile, AppShapes.iconTileRadius, AppShapes.iconTileIcon)
        Column(verticalArrangement = Arrangement.spacedBy(AppShapes.labelTitleGap), modifier = Modifier.weight(1f)) {
            label?.let { Text(it, style = AppType.label) }
            Text(title, style = AppType.cardTitle)
        }
        action?.invoke()
    }
}

@Composable
fun IconTile(@DrawableRes icon: Int, background: Color, tint: Color, size: Dp, radius: Dp, iconSize: Dp, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(background, RoundedCornerShape(radius))
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** A pill: [dot] before the text when given. */
@Composable
fun DesignChip(text: String, background: Color, color: Color, dot: Color? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(AppShapes.chipHeight)
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = AppShapes.chipPaddingHorizontal)
    ) {
        dot?.let {
            Box(Modifier.size(AppShapes.chipDot).background(it, CircleShape))
            Spacer(Modifier.width(AppShapes.chipDotGap))
        }
        Text(text, style = AppType.chip, color = color)
    }
}

/** The 50-high button: an icon, then the label, centred. */
@Composable
fun DesignButton(
    text: String,
    @DrawableRes icon: Int,
    background: Color,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppShapes.buttonIconGap, Alignment.CenterHorizontally),
        modifier = modifier
            .height(AppShapes.buttonHeight)
            .clip(RoundedCornerShape(AppShapes.buttonRadius))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = color, modifier = Modifier.size(AppShapes.buttonIcon))
        Text(text, style = AppType.button, color = color)
    }
}

/** Equal-width buttons side by side, [AppShapes.buttonRowGap] apart. */
@Composable
fun DesignButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppShapes.buttonRowGap),
        modifier = Modifier.fillMaxWidth(),
        content = content
    )
}

/** An icon-only button: [border] drawn when given; [description] read out instead of the icon. */
@Composable
fun SquareIconButton(
    @DrawableRes icon: Int,
    description: String,
    size: Dp,
    radius: Dp,
    iconSize: Dp,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: Color? = null
) {
    val shape = RoundedCornerShape(radius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(AppShapes.buttonBorder, border, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}
