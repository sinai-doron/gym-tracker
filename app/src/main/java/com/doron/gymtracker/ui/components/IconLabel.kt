package com.doron.gymtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A small icon before a label, tinted like the label: what the app draws where it once began a
 * label with an emoji, which looks different on every phone. The icon is decoration; the label
 * says what it means.
 */
@Composable
fun IconLabel(
    @DrawableRes icon: Int,
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    iconSize: Dp = 18.dp,
    maxLines: Int = Int.MAX_VALUE
) {
    val tint = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
        Spacer(Modifier.width(6.dp))
        Text(text, style = style, color = color, maxLines = maxLines)
    }
}
