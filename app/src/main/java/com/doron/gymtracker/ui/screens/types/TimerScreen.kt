package com.doron.gymtracker.ui.screens.types

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.R
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.TrainingTypes
import com.doron.gymtracker.domain.model.TrainingType
import com.doron.gymtracker.ui.components.TrainingTypeBadge
import com.doron.gymtracker.ui.components.displayName
import com.doron.gymtracker.ui.theme.Purple
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TimerState(
    val type: TrainingType? = null,
    /** The session being timed; null before Start. */
    val sessionId: Long? = null,
    val startedAt: Long? = null,
    val bodyWeightKg: Float? = null,
)

@HiltViewModel
class TimerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val sessions: SessionRepository,
    private val types: TrainingTypes,
    private val settings: SettingsPrefs,
    private val healthConnect: HealthConnectManager
) : ViewModel() {

    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // One timer at a time: one left running (the app was closed) comes back, whatever was tapped
            val open = sessions.getOpenTimedSession()
            val typeId = open?.activityTypeId ?: checkNotNull(savedState.get<String>("typeId"))
            _state.value = TimerState(
                type = types.get(typeId),
                sessionId = open?.id,
                startedAt = open?.startedAt,
                bodyWeightKg = settings.bodyWeightKg.first()
            )
        }
    }

    fun start() {
        val type = _state.value.type ?: return
        if (_state.value.sessionId != null) return
        viewModelScope.launch {
            val id = sessions.startTimedSession(type.id)
            _state.update { it.copy(sessionId = id, startedAt = sessions.getById(id)?.startedAt) }
        }
    }

    /** Stops the timer and saves the session, also to Health Connect as [title]. */
    fun finish(calories: Int?, note: String, title: String, onDone: () -> Unit) {
        val s = _state.value
        val id = s.sessionId ?: return
        val startedAt = s.startedAt ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            sessions.finishTimedSession(id, now, calories, note)
            s.type?.healthConnectType?.let { hc ->
                healthConnect.writeOtherSession(startedAt, now, title, hc, calories)
            }
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        val id = _state.value.sessionId
        viewModelScope.launch {
            if (id != null) sessions.deleteSession(id)
            onDone()
        }
    }
}

/**
 * A training type recorded with a timer: Start, then Finish with optional calories (estimated
 * from the type's MET and the body weight in Settings) and a note. The session is saved when it
 * starts, so closing the app doesn't lose it; opening any timer again brings it back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(onDone: () -> Unit, viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val type = state.type
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var finishing by remember { mutableStateOf(false) }
    var discarding by remember { mutableStateOf(false) }
    LaunchedEffect(state.startedAt) {
        while (state.startedAt != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    // Leaving keeps the timer running; home shows it
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                }
            )
        }
    ) { padding ->
        if (type == null) return@Scaffold
        val title = type.displayName()
        val startedAt = state.startedAt
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp)
        ) {
            TrainingTypeBadge(type, size = 88.dp)
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Text(
                clock(if (startedAt == null) 0 else (now - startedAt) / 1000),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = if (startedAt == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = { if (startedAt == null) viewModel.start() else finishing = true },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    stringResource(if (startedAt == null) R.string.timer_start else R.string.timer_finish),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (startedAt != null) {
                TextButton(onClick = { discarding = true }) {
                    Text(stringResource(R.string.timer_discard), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (finishing && startedAt != null) {
            FinishDialog(
                estimate = type.estimateCalories((now - startedAt) / 1000, state.bodyWeightKg),
                onDismiss = { finishing = false },
                onSave = { calories, note -> viewModel.finish(calories, note, title, onDone) }
            )
        }
        if (discarding) {
            AlertDialog(
                onDismissRequest = { discarding = false },
                title = { Text(stringResource(R.string.timer_discard_title)) },
                confirmButton = {
                    TextButton(onClick = { viewModel.discard(onDone) }) {
                        Text(stringResource(R.string.timer_discard), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { discarding = false }) { Text(stringResource(R.string.cancel)) } }
            )
        }
    }
}

@Composable
private fun FinishDialog(estimate: Int?, onDismiss: () -> Unit, onSave: (calories: Int?, note: String) -> Unit) {
    var calories by remember { mutableStateOf(estimate?.toString().orEmpty()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.timer_finish_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = { text -> calories = text.filter { it.isDigit() }.take(5) },
                    label = { Text(stringResource(R.string.timer_calories)) },
                    supportingText = { if (estimate != null) Text(stringResource(R.string.timer_calories_estimate)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(1000) },
                    label = { Text(stringResource(R.string.timer_note)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(calories.toIntOrNull(), note) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** H:MM:SS, or M:SS under an hour. */
internal fun clock(totalSecs: Long): String {
    val h = totalSecs / 3600
    val m = (totalSecs % 3600) / 60
    val s = totalSecs % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
