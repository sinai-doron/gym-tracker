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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.LocationPoint
import com.doron.gymtracker.data.LocationTracker
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.domain.model.RunTarget
import com.doron.gymtracker.domain.model.RunTargetType
import com.doron.gymtracker.ui.components.RunMap
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GpsLockState(
    val position: LocationPoint? = null,
    val isLocked: Boolean = false
) {
    val accuracy: Float? get() = position?.accuracy
}

@HiltViewModel
class GpsLockViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val sessionRepo: SessionRepository,
    private val settingsPrefs: SettingsPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(GpsLockState())
    val state: StateFlow<GpsLockState> = _state.asStateFlow()

    // Starts with the last target used
    val target: StateFlow<RunTarget> = settingsPrefs.runTarget
        .stateIn(viewModelScope, SharingStarted.Eagerly, RunTarget())

    fun setTarget(target: RunTarget) {
        viewModelScope.launch { settingsPrefs.setRunTarget(target) }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        viewModelScope.launch {
            locationTracker.locationUpdates().collect { point ->
                val acc = point.accuracy
                _state.value = GpsLockState(
                    position = point,
                    isLocked = acc != null && acc < LOCK_ACCURACY_M
                )
            }
        }
    }

    suspend fun createSession(type: String): Long {
        return sessionRepo.startCardioSession(type)
    }

    private companion object {
        const val LOCK_ACCURACY_M = 20f
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
    val target by viewModel.target.collectAsState()
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The map, with a dot that settles as the fix gets precise; a pulse until the first fix
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Purple.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                val position = gpsState.position
                if (position == null) {
                    SearchingIndicator()
                } else {
                    RunMap(
                        modifier = Modifier.fillMaxSize(),
                        position = position,
                        dotColor = if (gpsState.isLocked) SuccessGreen else Purple
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                if (gpsState.isLocked) "GPS מוכן!" else "מחפש GPS...",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = if (gpsState.isLocked) SuccessGreen else MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(6.dp))

            val accuracyText = gpsState.accuracy?.let { "דיוק: %.0f מ׳".format(it) } ?: "ממתין לאות..."
            Text(
                accuracyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!gpsState.isLocked && gpsState.accuracy != null) {
                Text(
                    "צא החוצה לקליטה טובה יותר",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            RunTargetPicker(target = target, onChange = viewModel::setTarget)

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Dropdown for the kind of target, with a +/- stepper for its value next to it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RunTargetPicker(target: RunTarget, onChange: (RunTarget) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = targetTypeLabel(target.type),
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("מטרה") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                RunTargetType.entries.forEach { type ->
                    // Following a saved route needs saved routes first
                    val available = type != RunTargetType.ROUTE
                    DropdownMenuItem(
                        text = {
                            Text(if (available) targetTypeLabel(type) else "${targetTypeLabel(type)} (בקרוב)")
                        },
                        enabled = available,
                        onClick = {
                            onChange(target.copy(type = type))
                            expanded = false
                        }
                    )
                }
            }
        }
        when (target.type) {
            RunTargetType.DISTANCE -> ValueStepper(
                text = "%s ק״מ".format(formatKm(target.distanceKm)),
                onMinus = {
                    onChange(target.copy(distanceKm = (target.distanceKm - RunTarget.DISTANCE_STEP_KM).coerceAtLeast(RunTarget.DISTANCE_STEP_KM)))
                },
                onPlus = { onChange(target.copy(distanceKm = target.distanceKm + RunTarget.DISTANCE_STEP_KM)) }
            )
            RunTargetType.DURATION -> ValueStepper(
                text = "${target.durationMin} דק׳",
                onMinus = {
                    onChange(target.copy(durationMin = (target.durationMin - RunTarget.DURATION_STEP_MIN).coerceAtLeast(RunTarget.DURATION_STEP_MIN)))
                },
                onPlus = { onChange(target.copy(durationMin = target.durationMin + RunTarget.DURATION_STEP_MIN)) }
            )
            else -> Unit
        }
    }
}

@Composable
private fun ValueStepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // In RTL the first child sits on the right, so "+" reads first
        FilledTonalIconButton(onClick = onPlus) { Text("+", style = MaterialTheme.typography.titleLarge) }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp)
        )
        FilledTonalIconButton(onClick = onMinus) { Text("−", style = MaterialTheme.typography.titleLarge) }
    }
}

private fun targetTypeLabel(type: RunTargetType): String = when (type) {
    RunTargetType.NONE -> "ללא מטרה"
    RunTargetType.DISTANCE -> "מרחק"
    RunTargetType.DURATION -> "זמן"
    RunTargetType.ROUTE -> "מסלול"
}

private fun formatKm(km: Float): String =
    if (km == km.toInt().toFloat()) km.toInt().toString() else "%.1f".format(km)

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
