package com.doron.gymtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLayoutDirection
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.theme.AppColors
import com.doron.gymtracker.ui.theme.AppShapes
import com.doron.gymtracker.ui.theme.AppType
import com.doron.gymtracker.ui.theme.designShadow

/**
 * Home's ⋮ button and its menu: logging a past session first and highlighted, then training
 * types, programmes, routes and achievements, then settings. The button stays pressed (white,
 * accent icon) while the menu is open.
 */
@Composable
fun HomeOverflowMenu(
    onLogSession: () -> Unit,
    onTrainingTypes: () -> Unit,
    onPrograms: () -> Unit,
    onRoutes: () -> Unit,
    onAchievements: () -> Unit,
    onSettings: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var button by remember { mutableStateOf(Rect.Zero) }
    Box(Modifier.onGloballyPositioned { button = Rect(it.positionOnScreen(), it.size.toSize()) }) {
        SquareIconButton(
            icon = R.drawable.ic_home_more,
            description = stringResource(R.string.home_menu),
            size = AppShapes.headerIconButton,
            radius = AppShapes.headerIconButtonRadius,
            iconSize = AppShapes.headerIconButtonIcon,
            background = if (open) AppColors.surface else Color.Transparent,
            tint = if (open) AppColors.accentStrength else AppColors.textSecondary,
            onClick = { open = true },
            modifier = if (open) Modifier.designShadow(AppShapes.headerIconButtonPressedShadow, AppShapes.headerIconButtonRadius) else Modifier
        )
        if (open) {
            val close = { open = false }
            fun pick(action: () -> Unit): () -> Unit = { open = false; action() }
            MenuPopup(button, onDismiss = close) {
                ActionRow(R.drawable.ic_home_plus, R.string.log_title, R.string.home_menu_log_hint, pick(onLogSession))
                MenuDivider()
                MenuRow(R.drawable.ic_home_grid, R.string.types_title, pick(onTrainingTypes))
                MenuRow(R.drawable.ic_home_clipboard, R.string.programs_title, pick(onPrograms))
                MenuRow(R.drawable.ic_home_map, R.string.routes_title, pick(onRoutes))
                MenuRow(R.drawable.ic_home_award, R.string.achievements_all, pick(onAchievements))
                MenuDivider()
                MenuRow(R.drawable.ic_home_settings, R.string.settings, pick(onSettings))
            }
        }
    }
}

/**
 * The menu in a popup over the whole window, so its shadow is never cut off and a tap anywhere
 * else closes it: its top [AppShapes.menuOffsetY] under the [button], its outer edge
 * [AppShapes.menuOffsetX] past the button's (the screen's edge side: left in Hebrew, right in English).
 */
@Composable
private fun MenuPopup(button: Rect, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(AppShapes.menuRadius)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // The popup's window can start below the status bar: place the menu by screen positions
    var origin by remember { mutableStateOf(Offset.Zero) }
    Popup(
        popupPositionProvider = WholeWindow,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Box(
            contentAlignment = AbsoluteAlignment.TopLeft,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { origin = it.positionOnScreen() }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(AppShapes.menuRowGap),
                modifier = Modifier
                    .absoluteOffset {
                        val outward = AppShapes.menuOffsetX.roundToPx()
                        val x = if (rtl) button.left.toInt() - outward
                        else button.right.toInt() + outward - AppShapes.menuWidth.roundToPx()
                        IntOffset(x - origin.x.toInt(), button.bottom.toInt() + AppShapes.menuOffsetY.roundToPx() - origin.y.toInt())
                    }
                    .width(AppShapes.menuWidth)
                    .designShadow(AppShapes.menuShadow, AppShapes.menuRadius)
                    .background(AppColors.surface, shape)
                    .border(AppShapes.menuBorder, AppColors.border, shape)
                    // Taps between rows stay in the menu
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    // As in CSS, the border takes room of its own: Compose draws it over the padding
                    .padding(AppShapes.menuBorder)
                    .padding(AppShapes.menuPadding)
            ) { content() }
        }
    }
}

/** At the window's top left, so the popup's coordinates are the window's. */
private object WholeWindow : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) = IntOffset.Zero
}

@Composable
private fun ActionRow(@DrawableRes icon: Int, @StringRes title: Int, @StringRes hint: Int, onClick: () -> Unit) {
    RowFrame(AppShapes.menuActionRowHeight, AppColors.tintStrength, onClick) {
        IconTile(icon, AppColors.accentStrength, AppColors.surface, AppShapes.smallIconTile, AppShapes.smallIconTileRadius, AppShapes.smallIconTileIcon)
        Column(verticalArrangement = Arrangement.spacedBy(AppShapes.menuActionTextGap)) {
            Text(stringResource(title), style = AppType.menuItemHighlighted)
            Text(stringResource(hint), style = AppType.hint)
        }
    }
}

@Composable
private fun MenuRow(@DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
    RowFrame(AppShapes.menuRowHeight, null, onClick) {
        IconTile(icon, AppColors.ground, AppColors.textSecondary, AppShapes.smallIconTile, AppShapes.smallIconTileRadius, AppShapes.smallIconTileIcon)
        Text(stringResource(title), style = AppType.menuItem)
    }
}

@Composable
private fun RowFrame(height: Dp, background: Color?, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(AppShapes.menuRowRadius)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppShapes.menuRowIconGap),
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .then(if (background != null) Modifier.background(background) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = AppShapes.menuRowPaddingHorizontal)
    ) { content() }
}

@Composable
private fun MenuDivider() {
    Box(
        Modifier
            .padding(horizontal = AppShapes.menuDividerHorizontal, vertical = AppShapes.menuDividerVertical)
            .fillMaxWidth()
            .height(AppShapes.menuBorder)
            .background(AppColors.border)
    )
}
