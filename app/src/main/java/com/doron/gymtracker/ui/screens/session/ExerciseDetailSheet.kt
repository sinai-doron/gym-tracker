package com.doron.gymtracker.ui.screens.session

import com.doron.gymtracker.ui.components.localizedName
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.DistanceSetRecord
import com.doron.gymtracker.domain.model.ProgressionHint
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.RepSetRecord
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.ui.components.ExerciseIllustration
import com.doron.gymtracker.ui.components.RestTimerOverlay
import com.doron.gymtracker.ui.components.SetLogRow
import com.doron.gymtracker.ui.theme.RestTimerColor
import com.doron.gymtracker.ui.components.weightStepText
import com.doron.gymtracker.ui.components.weightUnitLabel
import com.doron.gymtracker.ui.components.LocalWeightUnit
import com.doron.gymtracker.data.format
import com.doron.gymtracker.data.toKg
import com.doron.gymtracker.data.floorToPlate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.focus.onFocusChanged
import com.doron.gymtracker.AppLocale
import com.doron.gymtracker.data.db.dao.EntryNote
import com.doron.gymtracker.data.catalogue.ExerciseInstructions
import com.doron.gymtracker.ui.components.appLanguage
import com.doron.gymtracker.ui.components.DesignButton
import com.doron.gymtracker.ui.components.DesignSmallButton
import com.doron.gymtracker.ui.theme.AppColors
import java.text.DateFormat
import java.util.Date

@Composable
fun ExerciseDetailSheet(
    viewModel: ExerciseDetailViewModel,
    onEntryChanged: () -> Unit = {},
    onShowProgress: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    val inputs = remember { BringIntoViewRequester() }
    var inputsFocused by remember { mutableStateOf(false) }
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    // A focused field alone would scroll up just far enough to show itself, leaving the other
    // field and the buttons under the keyboard. The keys are what moves the block with it open.
    // Only for the set's own fields: the note below them scrolls itself into view.
    LaunchedEffect(state.editingSetIndex, state.completedSets.size, state.restCountdownSecs != null, inputsFocused) {
        snapshotFlow { ime.getBottom(density) }.collectLatest { keyboardHeight ->
            if (keyboardHeight > 0 && inputsFocused) {
                withFrameNanos { } // let the new height reach layout first
                inputs.bringIntoView()
            }
        }
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
    ) {
        ExerciseIllustration(exerciseId = state.exercise.id)
        val steps = ExerciseInstructions.pick(state.instructions, appLanguage())
        if (steps.isNotEmpty()) HowToDoIt(steps)
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.exercise.localizedName(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (onShowProgress != null) {
                TextButton(onClick = onShowProgress) {
                    Text(stringResource(R.string.progress))
                }
            }
        }
        if (state.exercise.isPerSide) {
            Text(stringResource(R.string.session_per_side_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.canKeepNote) KeptNote(state.keptNote, onSave = viewModel::keepNote)
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.sets_label), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.width(12.dp))
            FilledTonalButton(
                onClick = { viewModel.changeTargetSets(-1) },
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("−", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "${state.exercise.targetSets}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            FilledTonalButton(
                onClick = { viewModel.changeTargetSets(1) },
                shape = CircleShape,
                modifier = Modifier.size(40.dp),
                contentPadding = ButtonDefaults.TextButtonContentPadding
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
        }
        Spacer(Modifier.height(8.dp))

        state.completedSets.forEachIndexed { i, set ->
            SetLogRow(
                setNumber = i + 1,
                record = set,
                isEditing = state.editingSetIndex == i,
                onClick = { viewModel.startEditing(i) }
            )
        }

        // Progression hint — only shown for rep exercises
        val hintText = when (val h = state.progressionHint) {
            is ProgressionHint.Increase -> stringResource(R.string.session_hint_increase_by, weightStepText(h.addKg))
            is ProgressionHint.Stalled -> stringResource(R.string.session_hint_stalled)
            is ProgressionHint.OnTrack -> null
        }
        if (hintText != null) {
            Text(hintText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(16.dp))

        val editIdx = state.editingSetIndex
        val restSecs = state.restCountdownSecs
        if (state.skipped) {
            SkippedBanner(onUndo = { viewModel.setSkipped(false); onEntryChanged() })
        }
        // Resting, the next set's inputs stay in view under a one-row countdown
        if (restSecs != null && !state.skipped) {
            RestTimerOverlay(
                countdownSecs = restSecs,
                totalSecs = state.restTotalSecs,
                onDismiss = { viewModel.dismissRestTimer() },
                onAdjust = { delta -> viewModel.adjustRestTimer(delta) }
            )
            Spacer(Modifier.height(12.dp))
        }
        if (!state.skipped) Column(
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(inputs)
                .onFocusChanged { inputsFocused = it.hasFocus }
        ) {
            val editingSet = if (editIdx != null) state.completedSets.getOrNull(editIdx) else null

            when (state.exercise) {
                is RepExerciseDefinition -> RepSetInput(
                    isBodyweight = (state.exercise as RepExerciseDefinition).isBodyweight,
                    lastWeight = state.lastWeight,
                    roundToPlate = state.deloadPrefill,
                    editingSet = editingSet as? RepSetRecord,
                    onLog = { reps, weight ->
                        if (editIdx != null) {
                            viewModel.updateRepSet(editIdx, reps, weight)
                        } else {
                            viewModel.logRepSet(reps, weight)
                        }
                        onEntryChanged()
                    },
                    onCancelEdit = if (editIdx != null) {{ viewModel.cancelEditing() }} else null
                )
                is TimedExerciseDefinition -> if (editIdx != null) {
                    // A timed set has nothing to edit — it can only be deleted
                    OutlinedButton(
                        onClick = { viewModel.cancelEditing() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    TimedSetInput(
                        isRunning = state.timerRunning,
                        elapsedSecs = state.timerElapsedSecs,
                        onStart = { viewModel.startTimedSet() },
                        onStop = { viewModel.stopTimedSet(); onEntryChanged() }
                    )
                }
                is DistanceExerciseDefinition -> DistanceSetInput(
                    targetDistanceM = (state.exercise as DistanceExerciseDefinition).targetDistanceM,
                    editingSet = editingSet as? DistanceSetRecord,
                    onLog = { distanceM ->
                        if (editIdx != null) {
                            viewModel.updateDistanceSet(editIdx, distanceM)
                        } else {
                            viewModel.logDistanceSet(distanceM)
                        }
                        onEntryChanged()
                    },
                    onCancelEdit = if (editIdx != null) {{ viewModel.cancelEditing() }} else null
                )
            }

            if (editIdx != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.deleteSet(editIdx); onEntryChanged() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.session_delete_set))
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        if (state.canKeepNote || state.noteToday.isNotEmpty()) {
            NoteToday(state.noteToday, state.lastNote, onSave = viewModel::saveNoteToday)
        }
        if (!state.skipped && editIdx == null) {
            Spacer(Modifier.height(12.dp))
            DesignButton(
                text = stringResource(R.string.session_skip_today),
                icon = R.drawable.ic_skip,
                background = AppColors.ground,
                color = AppColors.textSecondary,
                onClick = { viewModel.setSkipped(true); onEntryChanged() },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

/** wger's steps for the exercise, in the app's language when translated; folded away until asked for. */
@Composable
private fun HowToDoIt(steps: List<String>) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(stringResource(R.string.exercise_how_to), style = MaterialTheme.typography.titleSmall)
        Icon(
            if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }
    if (open) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var number = 0
            steps.forEach { step ->
                // "Starting position:" heads the steps under it rather than being one
                if (step.trimEnd().endsWith(":") && step.length < 60) {
                    Text(step.trimEnd().removeSuffix(":"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
                } else {
                    number++
                    Row {
                        Text("$number.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(24.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** The note kept on the exercise, pinned under its name; written and changed in a dialog. */
@Composable
private fun KeptNote(note: String?, onSave: (String) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    if (note == null) {
        DesignSmallButton(
            text = stringResource(R.string.exercise_note_add),
            icon = R.drawable.ic_home_plus,
            background = AppColors.tintStrength,
            color = AppColors.accentStrength,
            onClick = { editing = true },
            modifier = Modifier.padding(top = 8.dp)
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
                .padding(start = 12.dp)
        ) {
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
            )
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.exercise_note_edit))
            }
        }
    }
    if (editing) {
        var text by rememberSaveable { mutableStateOf(note.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(stringResource(R.string.exercise_note)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text(stringResource(R.string.exercise_note_hint)) },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(onClick = { onSave(text); editing = false }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

/** The day's note on the sets, saved as it's typed, with the last session's note above it. */
@Composable
private fun NoteToday(saved: String, last: EntryNote?, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(saved) }
    LaunchedEffect(text) {
        if (text.trim() == saved) return@LaunchedEffect
        delay(600) // typing: save once it pauses
        onSave(text)
    }
    if (last != null) {
        val date = remember(last.startedAt) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, AppLocale.current()).format(Date(last.startedAt))
        }
        Text(
            stringResource(R.string.exercise_note_last_time, date, last.note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.exercise_note_today)) },
        modifier = Modifier.fillMaxWidth()
    )
}

/** Shown instead of the set's inputs while the exercise is left out of today's session. */
@Composable
private fun SkippedBanner(onUndo: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.ground, MaterialTheme.shapes.medium)
            .padding(12.dp)
    ) {
        Text(
            stringResource(R.string.session_skipped_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        DesignSmallButton(
            text = stringResource(R.string.session_undo_skip),
            icon = R.drawable.ic_undo,
            background = AppColors.surface,
            color = AppColors.accentStrength,
            onClick = onUndo
        )
    }
    Spacer(Modifier.height(16.dp))
}

// New sets start at the reps usually done, regardless of the plan's target
private const val DEFAULT_REPS = 10

@Composable
private fun RepSetInput(
    isBodyweight: Boolean,
    lastWeight: Float?,
    /** [lastWeight] is a deload's share of the usual weight: shown rounded down to a plate. */
    roundToPlate: Boolean = false,
    editingSet: RepSetRecord? = null,
    onLog: (Int, Float) -> Unit,
    onCancelEdit: (() -> Unit)? = null
) {
    // Typed and shown in kg or pounds; kept in kg
    val unit = LocalWeightUnit.current
    val initialWeight = if (lastWeight != null && lastWeight > 0f) {
        unit.format(if (roundToPlate) unit.floorToPlate(lastWeight) else lastWeight)
    } else ""
    // Saveable, so a half-typed set survives a trip to progress or a rotation
    var repsText by rememberSaveable(editingSet) {
        mutableStateOf(editingSet?.reps?.toString() ?: DEFAULT_REPS.toString())
    }
    var weightText by rememberSaveable(editingSet, lastWeight) {
        mutableStateOf(
            editingSet?.weightKg?.let { unit.format(it) } ?: initialWeight
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it },
            label = { Text(stringResource(R.string.reps)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Bodyweight exercises are logged by reps only (stored as 0 kg)
        if (!isBodyweight) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it },
                label = { Text(stringResource(R.string.weight_in, weightUnitLabel())) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCancelEdit != null) {
            OutlinedButton(
                onClick = onCancelEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
        Button(
            onClick = {
                val reps = repsText.toIntOrNull() ?: return@Button
                val weight = if (isBodyweight) 0f
                else weightText.replace(',', '.').toDoubleOrNull()?.let { unit.toKg(it) } ?: return@Button
                onLog(reps, weight)
                if (editingSet == null) {
                    repsText = DEFAULT_REPS.toString()
                }
            },
            modifier = Modifier.weight(1f),
            enabled = repsText.isNotBlank() && (isBodyweight || weightText.isNotBlank())
        ) {
            Text(stringResource(if (editingSet != null) R.string.session_update_set else R.string.log_set))
        }
    }
}

@Composable
private fun TimedSetInput(isRunning: Boolean, elapsedSecs: Int, onStart: () -> Unit, onStop: () -> Unit) {
    if (isRunning) {
        Text(
            "%02d:%02d".format(elapsedSecs / 60, elapsedSecs % 60),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Button(
        onClick = if (isRunning) onStop else onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(if (isRunning) R.string.stop_timer else R.string.start_timer))
    }
}

@Composable
private fun DistanceSetInput(
    targetDistanceM: Int,
    editingSet: DistanceSetRecord? = null,
    onLog: (Int) -> Unit,
    onCancelEdit: (() -> Unit)? = null
) {
    var distanceText by rememberSaveable(editingSet) {
        mutableStateOf(editingSet?.distanceM?.toString() ?: "")
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text(stringResource(R.string.session_distance_target, targetDistanceM)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(16.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (onCancelEdit != null) {
            OutlinedButton(
                onClick = onCancelEdit,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
        Button(
            onClick = {
                val dist = distanceText.toIntOrNull() ?: return@Button
                onLog(dist)
                if (editingSet == null) distanceText = ""
            },
            modifier = Modifier.weight(1f),
            enabled = distanceText.isNotBlank()
        ) {
            Text(stringResource(if (editingSet != null) R.string.session_update_set else R.string.log_set))
        }
    }
}
