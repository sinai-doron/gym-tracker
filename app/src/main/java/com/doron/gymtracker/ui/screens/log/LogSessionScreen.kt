package com.doron.gymtracker.ui.screens.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.components.displayName
import com.doron.gymtracker.ui.components.localizedName
import com.doron.gymtracker.ui.theme.Purple
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A session done without the phone: a class, a game, a run with the phone in the locker. Saved as
 * logged by hand, so streaks and totals count it and speed and distance records don't. A strength
 * session goes on to its workout, to fill in the sets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSessionScreen(
    onClose: () -> Unit,
    onStrength: (sessionId: Long, workoutId: String) -> Unit,
    viewModel: LogSessionViewModel = hiltViewModel()
) {
    val s by viewModel.state.collectAsState()
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    val title = s.type?.displayName().orEmpty()
    val problem = s.problem()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel)) }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(title, onStrength, onClose) },
                        enabled = problem == null && !s.saving
                    ) { Text(stringResource(R.string.save), color = if (problem == null) Purple else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Label(stringResource(R.string.log_type))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(s.types, key = { it.id }) { type ->
                    FilterChip(
                        selected = type.id == s.typeId,
                        onClick = { viewModel.update { it.copy(typeId = type.id) } },
                        label = { Text(type.displayName()) }
                    )
                }
            }

            if (s.isStrength) {
                Label(stringResource(R.string.log_workout))
                if (s.workouts.isEmpty()) {
                    Text(stringResource(R.string.log_no_programme), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(s.workouts, key = { it.id }) { workout ->
                            FilterChip(
                                selected = workout.id == s.workoutId,
                                onClick = { viewModel.update { it.copy(workoutId = workout.id) } },
                                label = { Text("${workout.code} · ${workout.localizedName()}") }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Field(stringResource(R.string.log_date), s.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), Modifier.weight(1f)) { pickingDate = true }
                Field(stringResource(R.string.log_started), s.start.format(DateTimeFormatter.ofPattern("HH:mm")), Modifier.weight(1f)) { pickingTime = true }
            }

            Label(stringResource(R.string.log_duration))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Number(stringResource(R.string.log_hours), s.hours, Modifier.weight(1f)) { v -> viewModel.update { it.copy(hours = v.take(2)) } }
                Number(stringResource(R.string.log_minutes), s.minutes, Modifier.weight(1f)) { v -> viewModel.update { it.copy(minutes = v.take(2)) } }
            }

            if (!s.isStrength) {
                Number(stringResource(R.string.log_distance), s.distance, Modifier.fillMaxWidth(), decimal = true) { v ->
                    viewModel.update { it.copy(distance = v.take(6)) }
                }
            }
            Number(
                stringResource(R.string.log_calories), s.calories, Modifier.fillMaxWidth(),
                hint = s.estimatedCalories?.let { stringResource(R.string.log_calories_estimate, it) }
            ) { v -> viewModel.update { it.copy(calories = v.take(5)) } }
            OutlinedTextField(
                value = s.note,
                onValueChange = { v -> viewModel.update { it.copy(note = v.take(1000)) } },
                label = { Text(stringResource(R.string.log_note)) },
                modifier = Modifier.fillMaxWidth()
            )
            if (s.healthConnectAvailable) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = s.saveToHealthConnect, onCheckedChange = { v -> viewModel.update { it.copy(saveToHealthConnect = v) } })
                    Text(stringResource(R.string.log_health_connect))
                }
            }
            when (problem) {
                LogProblem.IN_THE_FUTURE -> Text(stringResource(R.string.log_in_future), color = MaterialTheme.colorScheme.error)
                LogProblem.NO_DURATION -> Text(stringResource(R.string.log_no_duration), color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
            if (s.isStrength) {
                Text(
                    stringResource(R.string.log_strength_next),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingDate) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = s.date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { ms ->
                        viewModel.update { it.copy(date = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()) }
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } }
        ) { DatePicker(state = picker) }
    }
    if (pickingTime) {
        val picker = rememberTimePickerState(initialHour = s.start.hour, initialMinute = s.start.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.update { it.copy(start = LocalTime.of(picker.hour, picker.minute)) }
                    pickingTime = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Field(label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(value) }
    }
}

@Composable
private fun Number(label: String, value: String, modifier: Modifier, decimal: Boolean = false, hint: String? = null, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }) },
        label = { Text(label) },
        supportingText = hint?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
    )
}
