package com.doron.gymtracker.ui.screens.plan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import java.util.UUID

private enum class ExerciseKind { REPS, TIME, DISTANCE }

/** Edits an exercise, or creates one when [initial] is null (then the kind can be chosen). */
@Composable
fun ExerciseEditDialog(
    initial: ExerciseDefinition?,
    onDismiss: () -> Unit,
    onSave: (ExerciseDefinition) -> Unit
) {
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
    var repsText by remember { mutableStateOf(((initial as? RepExerciseDefinition)?.targetReps ?: 10).toString()) }
    var distanceText by remember {
        mutableStateOf(((initial as? DistanceExerciseDefinition)?.targetDistanceM ?: 30).toString())
    }
    var bodyweight by remember { mutableStateOf((initial as? RepExerciseDefinition)?.isBodyweight ?: false) }

    val reps = repsText.toIntOrNull()?.takeIf { it > 0 }
    val distance = distanceText.toIntOrNull()?.takeIf { it > 0 }
    val valid = name.isNotBlank() && when (kind) {
        ExerciseKind.REPS -> reps != null
        ExerciseKind.DISTANCE -> distance != null
        ExerciseKind.TIME -> true
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_exercise else R.string.edit_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    ExerciseKind.REPS -> NumberField(repsText, { repsText = it }, stringResource(R.string.target_reps))
                    ExerciseKind.DISTANCE -> NumberField(distanceText, { distanceText = it }, stringResource(R.string.target_distance))
                    ExerciseKind.TIME -> Unit
                }
                Stepper(
                    stringResource(R.string.rest_label),
                    "%d:%02d".format(restSecs / 60, restSecs % 60),
                    { restSecs = (restSecs - 15).coerceAtLeast(15) }
                ) { restSecs = (restSecs + 15).coerceAtMost(600) }
                if (kind == ExerciseKind.REPS) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = bodyweight, onCheckedChange = { bodyweight = it })
                        Text(stringResource(R.string.bodyweight_label))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    val id = initial?.id ?: "custom_${UUID.randomUUID()}"
                    val trimmed = name.trim()
                    onSave(
                        when (kind) {
                            ExerciseKind.REPS -> (initial as? RepExerciseDefinition)?.copy(
                                name = trimmed, targetSets = sets, restDurationSecs = restSecs,
                                targetReps = reps!!, isBodyweight = bodyweight
                            ) ?: RepExerciseDefinition(id, trimmed, sets, restSecs, reps!!, isBodyweight = bodyweight)
                            ExerciseKind.TIME -> (initial as? TimedExerciseDefinition)?.copy(
                                name = trimmed, targetSets = sets, restDurationSecs = restSecs
                            ) ?: TimedExerciseDefinition(id, trimmed, sets, restSecs)
                            ExerciseKind.DISTANCE -> (initial as? DistanceExerciseDefinition)?.copy(
                                name = trimmed, targetSets = sets, restDurationSecs = restSecs, targetDistanceM = distance!!
                            ) ?: DistanceExerciseDefinition(id, trimmed, sets, restSecs, distance!!)
                        }
                    )
                }
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun KindChip(label: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(stringResource(label)) })
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> onChange(new.filter { it.isDigit() }.take(4)) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
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
