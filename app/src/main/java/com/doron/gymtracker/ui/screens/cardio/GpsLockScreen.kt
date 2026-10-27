package com.doron.gymtracker.ui.screens.cardio

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.LocationTracker
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GpsLockState(
    val accuracy: Float? = null,
    val isLocked: Boolean = false,
    val satellites: Int = 0
)

@HiltViewModel
class GpsLockViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GpsLockState())
    val state: StateFlow<GpsLockState> = _state.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startListening() {
        viewModelScope.launch {
            locationTracker.locationUpdates().collect { point ->
                val acc = point.accuracy
                _state.value = GpsLockState(
                    accuracy = acc,
                    isLocked = acc != null && acc < 20f
                )
            }
        }
    }

    suspend fun createSession(type: String): Long {
        return sessionRepo.startCardioSession(type)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLockScreen(
    type: String,
    viewModel: GpsLockViewModel,
    onReady: (sessionId: Long, type: String) -> Unit,
    onBack: () -> Unit
) {
    val gpsState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val typeName = if (type == "RUNNING") "ריצה" else "הליכה"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(typeName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.2f))

            if (gpsState.isLocked) {
                LockedIndicator()
            } else {
                SearchingIndicator()
            }

            Spacer(Modifier.height(32.dp))

            Text(
                if (gpsState.isLocked) "GPS מוכן!" else "מחפש GPS...",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (gpsState.isLocked) SuccessGreen else MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            val accuracyText = gpsState.accuracy?.let { "דיוק: %.0f מ׳".format(it) } ?: "ממתין לאות..."
            Text(
                accuracyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            if (!gpsState.isLocked && gpsState.accuracy != null) {
                Text(
                    "צא החוצה לקליטה טובה יותר",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(0.3f))

            Button(
                onClick = {
                    scope.launch {
                        val sessionId = viewModel.createSession(type)
                        onReady(sessionId, type)
                    }
                },
                enabled = gpsState.isLocked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text(
                    if (type == "RUNNING") "🏃 התחל ריצה" else "🚶 התחל הליכה",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(Modifier.weight(0.15f))
        }
    }
}

@Composable
private fun SearchingIndicator() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .alpha(alpha)
                .border(3.dp, Purple.copy(alpha = 0.4f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Purple.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("📡", fontSize = 40.sp)
        }
    }
}

@Composable
private fun LockedIndicator() {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(SuccessGreen.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("✅", fontSize = 40.sp)
    }
}
