package com.doron.gymtracker.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R

/** A soundtrack lists this many tracks; the rest open under [SoundtrackToggle]. */
const val SOUNDTRACK_PREVIEW = 7

/** "Show N more ⌄" / "Show less ⌃" under a soundtrack longer than [SOUNDTRACK_PREVIEW]. */
@Composable
fun SoundtrackToggle(
    total: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    if (total <= SOUNDTRACK_PREVIEW) return
    TextButton(onClick = onToggle, modifier = modifier) {
        Text(
            if (expanded) stringResource(R.string.soundtrack_show_less)
            else stringResource(R.string.soundtrack_show_more, total - SOUNDTRACK_PREVIEW),
            color = color,
            style = style
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}
