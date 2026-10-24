package com.doron.gymtracker.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SessionClockDisplay(elapsedSecs: Long) {
    val minutes = elapsedSecs / 60
    val seconds = elapsedSecs % 60
    Text(
        text = "%02d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
