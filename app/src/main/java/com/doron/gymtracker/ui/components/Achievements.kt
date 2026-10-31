package com.doron.gymtracker.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.AchievementFamily
import com.doron.gymtracker.domain.model.AchievementStatus
import com.doron.gymtracker.ui.theme.Purple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** An achievement's artwork; greyed out and faded until it's earned, like the distance badges. */
@Composable
fun AchievementBadge(status: AchievementStatus, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(status.kind.image),
        contentDescription = stringResource(status.kind.title),
        colorFilter = if (status.isEarned) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .alpha(if (status.isEarned) 1f else 0.45f)
    )
}

/** Under a badge: the date it was earned, or how far along it is. */
@Composable
fun AchievementCaption(status: AchievementStatus, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.labelMedium
    if (status.isEarned) {
        Text(
            SimpleDateFormat("d.M.yy", Locale.getDefault()).format(Date(status.earnedAt!!)),
            style = style.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        return
    }
    val unit = when (status.kind.family) {
        AchievementFamily.CLIMBER -> R.string.achievement_progress_m
        AchievementFamily.MILEAGE -> R.string.achievement_progress_km
        AchievementFamily.IRON -> R.string.achievement_progress_t
        AchievementFamily.REGULAR -> R.string.achievement_progress_weeks
        AchievementFamily.SPECIAL -> null
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        if (unit == null) {
            Text(stringResource(R.string.achievement_not_yet), style = style, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LinearProgressIndicator(
                progress = { status.fraction },
                color = Purple,
                modifier = Modifier.width(72.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.height(3.dp))
            Text(
                stringResource(unit, number(status.progress), number(status.kind.target)),
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun number(value: Double): String =
    if (value >= 100 || value == value.toLong().toDouble()) "%,d".format(value.toLong()) else "%.1f".format(value)

/** One badge and its caption. */
@Composable
fun AchievementItem(status: AchievementStatus, size: Dp, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        AchievementBadge(status, size)
        Spacer(Modifier.height(4.dp))
        AchievementCaption(status)
    }
}

/**
 * The home card's strip: the most recently earned first, then the closest to being earned, so
 * it always shows what just happened and what is next.
 */
@Composable
fun AchievementsStrip(achievements: List<AchievementStatus>, modifier: Modifier = Modifier) {
    val ordered = achievements.filter { it.isEarned }.sortedByDescending { it.earnedAt } +
        achievements.filter { !it.isEarned }.sortedByDescending { it.fraction }
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
    ) {
        ordered.forEach { AchievementItem(it, size = 72.dp) }
    }
}

/** At the end of a run or a workout: what it just earned. */
@Composable
fun NewAchievementsDialog(achievements: List<AchievementStatus>, onDismiss: () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.3f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "achievement"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("יופי!", color = Purple) } },
        title = {
            Text(
                "🎉 " + stringResource(R.string.achievements_new),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    achievements.forEach {
                        AchievementBadge(it, size = if (achievements.size == 1) 160.dp else 96.dp, modifier = Modifier.scale(scale))
                    }
                }
                Spacer(Modifier.height(12.dp))
                achievements.forEach {
                    Text(stringResource(it.kind.title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
            }
        }
    )
}
