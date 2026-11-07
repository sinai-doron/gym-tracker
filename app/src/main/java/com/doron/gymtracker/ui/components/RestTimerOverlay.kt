package com.doron.gymtracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.theme.RestTimerColor

/**
 * The rest countdown as one row, so the exercise and the next set's inputs stay in view under it:
 * a ring that empties, the time left, -15 / +15 and Skip.
 */
@Composable
fun RestTimerOverlay(
    countdownSecs: Int,
    totalSecs: Int,
    onDismiss: () -> Unit,
    onAdjust: (deltaSecs: Int) -> Unit
) {
    val progress = if (totalSecs > 0) countdownSecs.toFloat() / totalSecs else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "rest-progress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val strokeWidth = 5.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = RestTimerColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.rest), style = MaterialTheme.typography.labelMedium, color = RestTimerColor)
            Text(
                "%d:%02d".format(countdownSecs / 60, countdownSecs % 60),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = RestTimerColor
            )
        }
        FilledTonalButton(
            onClick = { onAdjust(-15) },
            shape = CircleShape,
            modifier = Modifier.size(44.dp),
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text("-15", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(8.dp))
        FilledTonalButton(
            onClick = { onAdjust(15) },
            shape = CircleShape,
            modifier = Modifier.size(44.dp),
            contentPadding = ButtonDefaults.TextButtonContentPadding
        ) {
            Text("+15", style = MaterialTheme.typography.labelSmall)
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.skip_rest), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
