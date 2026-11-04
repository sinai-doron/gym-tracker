package com.doron.gymtracker.ui.screens.picker

import com.doron.gymtracker.ui.components.appLanguage
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.data.catalogue.LibraryVocabulary
import com.doron.gymtracker.domain.model.LibraryExercise
import com.doron.gymtracker.ui.components.BodyMapFigures
import com.doron.gymtracker.ui.components.localizedName
import com.doron.gymtracker.ui.screens.plan.ExerciseTargetsSheet
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen

/**
 * Adds an exercise to a workout from the library: search by any name or alias, filter by muscle (on
 * the body map or by name) and equipment, the ones done lately first. Picking one opens its targets, filled with its defaults.
 * Not there: create one (a user's own exercise), after being shown similar ones already there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerScreen(
    onDone: () -> Unit,
    viewModel: ExercisePickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang = appLanguage()
    LaunchedEffect(lang) { viewModel.load(lang) }
    var picked by remember { mutableStateOf<LibraryExercise?>(null) }
    var creating by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = creating != null) { creating = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (creating != null) stringResource(R.string.create_title)
                        else stringResource(R.string.picker_title, state.workout?.localizedName() ?: "")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = { if (creating != null) creating = null else onDone() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val all = state.all ?: return@Scaffold
        val modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .navigationBarsPadding()
            .imePadding()
        val initialName = creating
        if (initialName != null) {
            CreateExercisePane(
                initialName = initialName,
                similar = viewModel::similar,
                onUseExisting = { picked = it; creating = null },
                onSave = { name, preset, equipment, muscle -> viewModel.createAndAdd(name, preset, equipment, muscle, onDone) },
                modifier = modifier
            )
        } else {
            PickerList(
                state = state,
                total = all.size,
                onQuery = viewModel::setQuery,
                onMuscle = viewModel::toggleMuscle,
                onRegion = viewModel::toggleRegion,
                onEquipment = viewModel::toggleEquipment,
                onPick = { picked = it },
                onCreate = { creating = state.query.trim() },
                modifier = modifier
            )
        }
    }

    picked?.let { exercise ->
        ExerciseTargetsSheet(
            initial = remember(exercise.id) { viewModel.draft(exercise) },
            isNew = true,
            onDismiss = { picked = null },
            onSave = { definition ->
                picked = null
                viewModel.add(definition, exercise.id, onDone)
            }
        )
    }
}

@Composable
private fun PickerList(
    state: PickerUiState,
    total: Int,
    onQuery: (String) -> Unit,
    onMuscle: (String) -> Unit,
    onRegion: (List<String>) -> Unit,
    onEquipment: (String) -> Unit,
    onPick: (LibraryExercise) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier
) {
    val results = state.results
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                placeholder = { Text(stringResource(R.string.picker_search, total)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
        // The map makes room for the results while a name is being typed
        if (state.query.isBlank()) {
            item(key = "body") { BodyMapFigures(state.muscles, onRegion, Modifier.padding(top = 4.dp)) }
        }
        item(key = "muscles") {
            ChipRow(LibraryVocabulary.MUSCLES, state.muscles, LibraryVocabulary::muscleLabel, onMuscle)
        }
        item(key = "equipment") {
            ChipRow(LibraryVocabulary.EQUIPMENT_STEP_KG.keys.toList(), state.equipment, LibraryVocabulary::equipmentLabel, onEquipment)
        }
        item(key = "count") {
            Text(
                if (state.showingRecent) stringResource(R.string.picker_recent)
                else pluralStringResource(R.plurals.picker_count, results.size, results.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (results.isEmpty()) {
            item(key = "nothing") { Text(stringResource(R.string.picker_nothing), style = MaterialTheme.typography.bodyMedium) }
        }
        items(results, key = { it.id }) { exercise -> ExerciseRow(exercise, onClick = { onPick(exercise) }) }
        item(key = "create") {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(stringResource(R.string.picker_not_here), color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onCreate, modifier = Modifier.padding(top = 4.dp)) {
                    Text("+ " + stringResource(R.string.picker_create), color = Purple)
                }
            }
        }
    }
}

@Composable
private fun ChipRow(slugs: List<String>, selected: Set<String>, label: (String) -> Int?, onToggle: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(slugs, key = { it }) { slug ->
            FilterChip(
                selected = slug in selected,
                onClick = { onToggle(slug) },
                label = { Text(label(slug)?.let { stringResource(it) } ?: slug) }
            )
        }
    }
}

@Composable
private fun ExerciseRow(exercise: LibraryExercise, onClick: () -> Unit) {
    val muscles = exercise.primaryMuscles.mapNotNull { LibraryVocabulary.muscleLabel(it)?.let { id -> stringResource(id) } }
    val equipment = LibraryVocabulary.equipmentLabel(exercise.equipment)?.let { stringResource(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClickLabel = stringResource(R.string.picker_add, exercise.name), onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(exercise.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(
                (muscles + listOfNotNull(equipment)).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (exercise.isCustom) {
            Text(
                stringResource(R.string.picker_yours),
                style = MaterialTheme.typography.labelSmall,
                color = SuccessGreen,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }
        Icon(Icons.Default.Add, contentDescription = null, tint = Purple)
    }
}

/** The presets a user's own exercise can have, most common first. */
private val CREATE_PRESETS = listOf("reps_load", "reps", "reps_assisted", "time", "distance_load")

// Chip rows scroll sideways rather than wrap: FlowRow's signature differs between the Compose the
// app compiles against and the one it runs with, and calling it crashed.
@Composable
private fun CreateExercisePane(
    initialName: String,
    similar: (String) -> List<LibraryExercise>,
    onUseExisting: (LibraryExercise) -> Unit,
    onSave: (name: String, preset: String, equipment: String, muscle: String) -> Unit,
    modifier: Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var preset by remember { mutableStateOf(CREATE_PRESETS.first()) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var muscle by remember { mutableStateOf<String?>(null) }
    val alike = remember(name) { similar(name) }
    var saving by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.create_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (alike.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(stringResource(R.string.create_similar), style = MaterialTheme.typography.bodySmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(alike, key = { it.id }) { e -> SuggestionChip(onClick = { onUseExisting(e) }, label = { Text(e.name) }) }
                }
            }
        }
        Label(stringResource(R.string.create_records))
        ChipRow(CREATE_PRESETS, setOf(preset), LibraryVocabulary::presetLabel) { preset = it }
        Label(stringResource(R.string.picker_equipment))
        ChipRow(LibraryVocabulary.EQUIPMENT_STEP_KG.keys.toList(), setOfNotNull(equipment), LibraryVocabulary::equipmentLabel) { equipment = it }
        Label(stringResource(R.string.create_muscle))
        ChipRow(LibraryVocabulary.MUSCLES, setOfNotNull(muscle), LibraryVocabulary::muscleLabel) { muscle = it }
        val chosenEquipment = equipment
        val chosenMuscle = muscle
        Button(
            enabled = !saving && name.isNotBlank() && chosenEquipment != null && chosenMuscle != null,
            onClick = {
                if (chosenEquipment != null && chosenMuscle != null) {
                    saving = true
                    onSave(name.trim(), preset, chosenEquipment, chosenMuscle)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text(stringResource(R.string.create_save))
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
