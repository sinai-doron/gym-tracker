package com.doron.gymtracker.ui.screens.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.VariantRule
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.ui.components.localizedName
import com.doron.gymtracker.ui.theme.Purple
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** A workout being added (null) or edited. */
private data class WorkoutEdit(val workout: Workout?)

/**
 * One programme: its workouts in the order they rotate, and when its week 1 starts. Tapping a
 * workout opens its exercises (the plan editor). Rotation, deload and alternate weeks show as
 * they are; settings for them come with the rules engine (roadmap phase 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramBuilderScreen(
    onBack: () -> Unit,
    onOpenWorkout: (workoutId: String) -> Unit,
    viewModel: ProgramBuilderViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<WorkoutEdit?>(null) }
    var removing by remember { mutableStateOf<Workout?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reload() }

    val program = state.program
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(program?.displayName() ?: "") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (program != null) {
                        TextButton(onClick = { renaming = true }) { Text(stringResource(R.string.builder_rename), color = Purple) }
                    }
                }
            )
        }
    ) { padding ->
        if (program == null) return@Scaffold
        val workouts = state.workouts
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "header") { SectionLabel(stringResource(R.string.builder_workouts_header)) }
            if (workouts.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.builder_no_workouts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(workouts, key = { _, w -> w.id }) { i, workout ->
                WorkoutRow(
                    workout = workout,
                    canMoveUp = i > 0,
                    canMoveDown = i < workouts.lastIndex,
                    onOpen = { onOpenWorkout(workout.id) },
                    onMoveUp = { viewModel.moveWorkout(workout.id, -1) },
                    onMoveDown = { viewModel.moveWorkout(workout.id, 1) },
                    onEdit = { editing = WorkoutEdit(workout) },
                    onRemove = { removing = workout }
                )
            }
            item(key = "add") {
                OutlinedButton(
                    onClick = { editing = WorkoutEdit(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("+ " + stringResource(R.string.builder_add_workout), color = Purple)
                }
            }
            item(key = "schedule") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    SectionLabel(stringResource(R.string.builder_schedule))
                    InfoField(
                        label = stringResource(R.string.builder_start_date),
                        value = startValue(program),
                        onClick = { pickingDate = true }
                    )
                    InfoField(label = stringResource(R.string.builder_rules), value = rulesValue(program), onClick = null)
                    Text(
                        stringResource(R.string.builder_rules_later),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (renaming && program != null) {
        NameDialog(
            title = stringResource(R.string.builder_rename),
            label = stringResource(R.string.builder_program_name),
            initial = program.displayName(),
            onDismiss = { renaming = false },
            onSave = { name ->
                renaming = false
                viewModel.rename(name)
            }
        )
    }

    editing?.let { edit ->
        WorkoutDialog(
            initial = edit.workout,
            suggestedCode = nextCode(state.workouts),
            onDismiss = { editing = null },
            onSave = { code, name ->
                editing = null
                val workout = edit.workout
                if (workout == null) viewModel.addWorkout(code, name) else viewModel.editWorkout(workout.id, code, name)
            }
        )
    }

    removing?.let { workout ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.builder_remove_workout_title, workout.localizedName())) },
            text = { Text(stringResource(R.string.builder_remove_workout_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeWorkout(workout.id)
                    removing = null
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (pickingDate && program != null) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = program.startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let {
                        viewModel.setStartDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    pickingDate = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = picker)
        }
    }
}

/** The next free letter for a new workout's code: A, B, C… */
private fun nextCode(workouts: List<Workout>): String {
    val used = workouts.map { it.code }.toSet()
    return ('A'..'Z').map { it.toString() }.firstOrNull { it !in used } ?: ""
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun WorkoutRow(
    workout: Workout,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val name = workout.localizedName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClickLabel = stringResource(R.string.builder_open_workout, name), onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier
            .weight(1f)
            .padding(start = 4.dp)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            val count = workout.exercises.size
            Text(
                pluralStringResource(R.plurals.builder_exercises, count, count) +
                    if (count > 0) " · " + stringResource(R.string.builder_about_minutes, workout.estimatedMinutes()) else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column {
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.builder_edit_workout), tint = Purple)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoField(label: String, value: String, onClick: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun startValue(program: Program): String {
    val date = program.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val week = program.weekNumber(System.currentTimeMillis())
    return if (week >= 1 && !program.startDate.isAfter(LocalDate.now())) stringResource(R.string.builder_start_value, date, week) else date
}

@Composable
private fun rulesValue(program: Program): String = listOf(
    stringResource(R.string.builder_rules_in_order),
    program.deloadEveryWeeks?.let { stringResource(R.string.builder_rules_deload_every, it) }
        ?: stringResource(R.string.builder_rules_no_deload),
    stringResource(
        if (program.variantRule == VariantRule.ALTERNATE_WEEKLY) R.string.builder_rules_alternate
        else R.string.builder_rules_no_variants
    )
).joinToString(" · ")

/**
 * A workout's name. The code it is stored with is no longer shown: a new workout gets the next
 * free letter, an edited one keeps its own.
 */
@Composable
private fun WorkoutDialog(
    initial: Workout?,
    suggestedCode: String,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String?) -> Unit
) {
    // A built-in workout's translated name stays unset, so it still follows the app's language
    val current = initial?.localizedName()
    val builtInName = current.takeIf { initial?.name == null }
    var name by remember { mutableStateOf(current ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.builder_add_workout else R.string.builder_edit_workout)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.builder_workout_name)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(initial?.code ?: suggestedCode, name.trim().takeIf { it != builtInName }) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

