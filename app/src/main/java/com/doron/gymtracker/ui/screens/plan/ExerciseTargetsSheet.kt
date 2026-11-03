package com.doron.gymtracker.ui.screens.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.ui.theme.Purple
import java.util.UUID

private enum class ExerciseKind { REPS, TIME, DISTANCE }

/**
 * An exercise's targets in a workout: sets, reps (a range or a fixed count), rest, and whether it's
 * optional or done per side. Creates one when [initial] is null; then the kind can be chosen. With
 * [isNew] and an [initial], adds that exercise (one picked from the library) with its targets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseTargetsSheet(
    initial: ExerciseDefinition?,
    onDismiss: () -> Unit,
    onSave: (ExerciseDefinition) -> Unit,
    isNew: Boolean = initial == null
) {
    val rep = initial as? RepExerciseDefinition
    var kind by remember {
        mutableStateOf(
            when (initial) {
                is TimedExerciseDefinition -> ExerciseKind.TIME
                is DistanceExerciseDefinition -> ExerciseKind.DISTANCE
                else -> ExerciseKind.REPS
            }
        )
    }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sets by remember { mutableIntStateOf(initial?.targetSets ?: 3) }
    var restSecs by remember { mutableIntStateOf(initial?.restDurationSecs ?: 90) }
    var repsMax by remember { mutableIntStateOf(rep?.targetReps ?: 10) }
    var repsMin by remember { mutableIntStateOf(rep?.targetRepsMin ?: rep?.targetReps ?: 10) }
    var distanceText by remember {
        mutableStateOf(((initial as? DistanceExerciseDefinition)?.targetDistanceM ?: 30).toString())
    }
    var bodyweight by remember { mutableStateOf(rep?.isBodyweight ?: false) }
    var optional by remember { mutableStateOf(initial?.isOptional ?: false) }
    var perSide by remember { mutableStateOf(initial?.isPerSide ?: false) }

    val distance = distanceText.toIntOrNull()?.takeIf { it > 0 }
    val valid = name.isNotBlank() && (kind != ExerciseKind.DISTANCE || distance != null)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(if (isNew) R.string.add_exercise else R.string.edit_exercise),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            // The kind decides how sets are logged, so it's fixed once an exercise exists
            if (initial == null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KindChip(R.string.kind_reps, kind == ExerciseKind.REPS) { kind = ExerciseKind.REPS }
                    KindChip(R.string.kind_time, kind == ExerciseKind.TIME) { kind = ExerciseKind.TIME }
                    KindChip(R.string.kind_distance, kind == ExerciseKind.DISTANCE) { kind = ExerciseKind.DISTANCE }
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.exercise_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Stepper(stringResource(R.string.sets_label), "$sets", { sets = (sets - 1).coerceAtLeast(1) }) {
                sets = (sets + 1).coerceAtMost(10)
            }
            when (kind) {
                ExerciseKind.REPS -> {
                    // A range: the bottom can't pass the top, and moving the top below it brings it along
                    Stepper(
                        stringResource(R.string.targets_reps_from), "$repsMin",
                        { repsMin = (repsMin - 1).coerceAtLeast(1) }
                    ) { repsMin = (repsMin + 1).coerceAtMost(repsMax) }
                    Stepper(
                        stringResource(R.string.targets_reps_to), "$repsMax",
                        { repsMax = (repsMax - 1).coerceAtLeast(1); repsMin = repsMin.coerceAtMost(repsMax) }
                    ) { repsMax = (repsMax + 1).coerceAtMost(MAX_REPS) }
                    Text(
                        stringResource(R.string.targets_reps_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ExerciseKind.DISTANCE -> OutlinedTextField(
                    value = distanceText,
                    onValueChange = { new -> distanceText = new.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.target_distance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExerciseKind.TIME -> Unit
            }
            Stepper(
                stringResource(R.string.rest_label),
                "%d:%02d".format(restSecs / 60, restSecs % 60),
                { restSecs = (restSecs - 15).coerceAtLeast(15) }
            ) { restSecs = (restSecs + 15).coerceAtMost(600) }
            Toggle(stringResource(R.string.targets_optional), optional) { optional = it }
            Toggle(stringResource(R.string.targets_per_side), perSide) { perSide = it }
            if (kind == ExerciseKind.REPS) Toggle(stringResource(R.string.bodyweight_label), bodyweight) { bodyweight = it }

            Button(
                enabled = valid,
                onClick = {
                    val id = initial?.id ?: "custom_${UUID.randomUUID()}"
                    val trimmed = name.trim()
                    onSave(
                        when (kind) {
                            ExerciseKind.REPS -> {
                                val min = repsMin.takeIf { it < repsMax }
                                rep?.copy(
                                    name = trimmed, targetSets = sets, restDurationSecs = restSecs, targetReps = repsMax,
                                    targetRepsMin = min, isBodyweight = bodyweight, isOptional = optional, isPerSide = perSide
                                ) ?: RepExerciseDefinition(
                                    id, trimmed, sets, restSecs, repsMax, isBodyweight = bodyweight,
                                    isOptional = optional, isPerSide = perSide, targetRepsMin = min
                                )
                            }
                            ExerciseKind.TIME -> (initial as? TimedExerciseDefinition)?.copy(
                                name = trimmed, targetSets = sets, restDurationSecs = restSecs, isOptional = optional, isPerSide = perSide
                            ) ?: TimedExerciseDefinition(id, trimmed, sets, restSecs, isOptional = optional, isPerSide = perSide)
                            ExerciseKind.DISTANCE -> (initial as? DistanceExerciseDefinition)?.copy(
                                name = trimmed, targetSets = sets, restDurationSecs = restSecs, targetDistanceM = distance!!,
                                isOptional = optional, isPerSide = perSide
                            ) ?: DistanceExerciseDefinition(id, trimmed, sets, restSecs, distance!!, isOptional = optional, isPerSide = perSide)
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(52.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text(stringResource(if (isNew) R.string.targets_add else R.string.save))
            }
        }
    }
}

private const val MAX_REPS = 100

@Composable
private fun KindChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Stepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        StepButton("−", onMinus)
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
        StepButton("+", onPlus)
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier.size(40.dp),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}
