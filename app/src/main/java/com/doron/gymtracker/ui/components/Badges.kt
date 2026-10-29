package com.doron.gymtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.BadgeStatus
import com.doron.gymtracker.domain.model.RunBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@get:DrawableRes
val RunBadge.image: Int
    get() = when (this) {
        RunBadge.ONE_K -> R.drawable.badge_1k
        RunBadge.FIVE_K -> R.drawable.badge_5k
        RunBadge.TEN_K -> R.drawable.badge_10k
    }

/** A badge's artwork; greyed out and faded until it's earned. */
@Composable
fun BadgeImage(badge: RunBadge, earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(badge.image),
        contentDescription = badge.label,
        colorFilter = if (earned) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .alpha(if (earned) 1f else 0.45f)
    )
}

/** Every badge, each with the date it was earned or "not yet". */
@Composable
fun BadgesRow(badges: List<BadgeStatus>, modifier: Modifier = Modifier) {
    val dateFormat = SimpleDateFormat("d.M.yy", Locale.getDefault())
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        badges.forEach { status ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BadgeImage(status.badge, status.isEarned, size = 88.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    status.earnedAt?.let { dateFormat.format(Date(it)) } ?: "טרם הושג",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (status.isEarned) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
