package com.doron.gymtracker.ui.screens.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.Program
import com.doron.gymtracker.domain.model.ProgramSummary
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Every programme: the active one first, the others ready to switch to, archived ones last. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    onBack: () -> Unit,
    onOpen: (programId: String) -> Unit,
    viewModel: ProgramsViewModel = hiltViewModel()
) {
    val programs by viewModel.programs.collectAsState()
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProgramSummary?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reload() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.programs_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val all = programs ?: return@Scaffold
        val (archived, current) = all.partition { it.archivedAt != null }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(current, key = { it.program.id }) { summary ->
                ProgramCard(
                    summary = summary,
                    onOpen = { onOpen(summary.program.id) },
                    onMakeActive = { viewModel.makeActive(summary.program.id) },
                    onArchive = { viewModel.archive(summary.program.id) },
                    onRestore = null,
                    onDelete = { deleting = summary }
                )
            }
            if (archived.isNotEmpty()) {
                item(key = "archived") {
                    Text(
                        stringResource(R.string.program_archived_header),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(archived, key = { it.program.id }) { summary ->
                    ProgramCard(
                        summary = summary,
                        onOpen = { onOpen(summary.program.id) },
                        onMakeActive = null,
                        onArchive = null,
                        onRestore = { viewModel.restore(summary.program.id) },
                        onDelete = { deleting = summary }
                    )
                }
            }
            item(key = "new") {
                OutlinedButton(
                    onClick = { naming = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(52.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("+ " + stringResource(R.string.program_new), color = Purple)
                }
            }
            item(key = "note") {
                Text(
                    stringResource(R.string.program_switch_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }

    if (naming) {
        val default = stringResource(R.string.program_new_name)
        NameDialog(
            title = stringResource(R.string.program_new),
            label = stringResource(R.string.builder_program_name),
            initial = default,
            onDismiss = { naming = false },
            onSave = { name ->
                naming = false
                viewModel.create(name.trim().ifEmpty { default }, onOpen)
            }
        )
    }

    deleting?.let { summary ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text(stringResource(R.string.program_delete_title, summary.program.displayName())) },
            text = { Text(stringResource(R.string.program_delete_text)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(summary.program.id)
                    deleting = null
                }) { Text(stringResource(R.string.program_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

/** The name the user gave it, else the seeded plan's. */
@Composable
fun Program.displayName(): String = name ?: stringResource(R.string.program_default_name)

/** Null actions aren't offered: a programme is archived or restored, made active or not. */
@Composable
private fun ProgramCard(
    summary: ProgramSummary,
    onOpen: () -> Unit,
    onMakeActive: (() -> Unit)?,
    onArchive: (() -> Unit)?,
    onRestore: (() -> Unit)?,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val name = summary.program.displayName()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (summary.archivedAt != null) 0.65f else 1f)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(
                programSummaryLine(summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        when {
            summary.isActive -> Text(
                stringResource(R.string.program_active),
                style = MaterialTheme.typography.labelMedium,
                color = SuccessGreen,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            onMakeActive != null -> TextButton(onClick = onMakeActive) {
                Text(stringResource(R.string.program_make_active), color = Purple)
            }
            onRestore != null -> TextButton(onClick = onRestore) {
                Text(stringResource(R.string.program_restore), color = Purple)
            }
        }
        val canArchive = onArchive != null && !summary.isActive
        if (canArchive || summary.canDelete) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.program_more, name))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    if (canArchive) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.program_archive)) },
                            onClick = { menu = false; onArchive?.invoke() }
                        )
                    }
                    if (summary.canDelete) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.program_delete), color = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun programSummaryLine(summary: ProgramSummary): String = listOfNotNull(
    pluralStringResource(R.plurals.program_workouts, summary.workoutCount, summary.workoutCount),
    stringResource(
        R.string.program_since,
        summary.program.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    ),
    summary.sessionCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.program_sessions, it, it) }
).joinToString(" · ")

/** One text field, for a programme's name. */
@Composable
fun NameDialog(title: String, label: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text(label) }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
