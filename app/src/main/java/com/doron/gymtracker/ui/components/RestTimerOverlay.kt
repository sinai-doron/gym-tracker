package com.doron.gymtracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("מנוחה", style = MaterialTheme.typography.titleMedium, color = RestTimerColor)

        Spacer(Modifier.height(16.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 10.dp.toPx()
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
            Text(
                "%d:%02d".format(countdownSecs / 60, countdownSecs % 60),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = RestTimerColor
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { onAdjust(-15) },
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("-15", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(16.dp))
            FilledTonalButton(
                onClick = { onAdjust(15) },
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("+15", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.skip_rest), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
