package com.doron.gymtracker.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.DistanceExerciseDefinition
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.RepExerciseDefinition
import com.doron.gymtracker.domain.model.TimedExerciseDefinition
import com.doron.gymtracker.ui.theme.Purple

/** The editor's dialog: editing an existing exercise, or adding a new one. */
private sealed interface EditTarget {
    data class Existing(val exercise: ExerciseDefinition) : EditTarget
    data object New : EditTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    onBack: () -> Unit,
    viewModel: PlanEditorViewModel = hiltViewModel()
) {
    val workout by viewModel.workout.collectAsState()
    var editing by remember { mutableStateOf<EditTarget?>(null) }
    var removing by remember { mutableStateOf<ExerciseDefinition?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(workout?.let { stringResource(R.string.edit_workout, it.label.name, it.label.displayName) } ?: "")
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val exercises = workout?.exercises ?: return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(exercises, key = { _, ex -> ex.id }) { i, exercise ->
                    PlanExerciseRow(
                        exercise = exercise,
                        canMoveUp = i > 0,
                        canMoveDown = i < exercises.lastIndex,
                        onMoveUp = { viewModel.move(exercise.id, -1) },
                        onMoveDown = { viewModel.move(exercise.id, 1) },
                        onEdit = { editing = EditTarget.Existing(exercise) },
                        onRemove = { removing = exercise }
                    )
                }
            }
            Button(
                onClick = { editing = EditTarget.New },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text("+ " + stringResource(R.string.add_exercise), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    editing?.let { target ->
        val initial = (target as? EditTarget.Existing)?.exercise
        ExerciseEditDialog(
            initial = initial,
            onDismiss = { editing = null },
            onSave = { exercise ->
                viewModel.save(exercise, isNew = initial == null)
                editing = null
            }
        )
    }

    removing?.let { exercise ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text(stringResource(R.string.remove_exercise_title, exercise.name)) },
            text = { Text(stringResource(R.string.remove_exercise_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(exercise.id)
                    removing = null
                }) { Text(stringResource(R.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun PlanExerciseRow(
    exercise: ExerciseDefinition,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(
                exerciseSummary(exercise),
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
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_exercise), tint = Purple)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun exerciseSummary(exercise: ExerciseDefinition): String {
    // LTR isolate keeps "4×6" in order inside RTL text
    val target = when (exercise) {
        is RepExerciseDefinition -> "\u2066${exercise.targetSets}×${exercise.targetReps}\u2069" +
            if (exercise.isBodyweight) " · " + stringResource(R.string.bodyweight_label) else ""
        is DistanceExerciseDefinition -> "\u2066${exercise.targetSets}×\u2069" +
            stringResource(R.string.distance_value, exercise.targetDistanceM)
        is TimedExerciseDefinition -> stringResource(R.string.sets_count, exercise.targetSets)
    }
    val rest = "%d:%02d".format(exercise.restDurationSecs / 60, exercise.restDurationSecs % 60)
    return "$target · " + stringResource(R.string.rest_value, rest)
}
