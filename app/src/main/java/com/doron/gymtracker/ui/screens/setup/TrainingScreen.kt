package com.doron.gymtracker.ui.screens.setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.theme.Purple
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Your training": start from a template or from an empty programme. Picking a template opens its
 * start date in place. On first launch ([firstLaunch]) it is the last step and can be skipped;
 * from Programmes or home it makes another programme. Either way the new one becomes active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingScreen(
    firstLaunch: Boolean,
    onLeave: () -> Unit,
    onTemplate: (programId: String) -> Unit,
    onEmpty: (programId: String) -> Unit,
    viewModel: TrainingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val lang = LocalConfiguration.current.locales[0].language.let { if (it == "iw") "he" else it }
    val emptyName = stringResource(R.string.setup_empty_name)
    var pickingDate by remember { mutableStateOf(false) }
    LaunchedEffect(state.hasProgramme) { if (firstLaunch && state.hasProgramme) onLeave() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { if (!firstLaunch) Text(stringResource(R.string.program_new)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    if (!firstLaunch) {
                        IconButton(onClick = onLeave) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                    }
                },
                actions = {
                    if (firstLaunch) {
                        TextButton(onClick = onLeave) {
                            Text(stringResource(R.string.setup_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.create(lang, emptyName, onTemplate, onEmpty) },
                enabled = state.chosen != null && !state.saving,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .height(56.dp)
            ) {
                Text(stringResource(R.string.setup_create), style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "title") {
                Text(stringResource(R.string.setup_training_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.setup_training_text),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
            }
            items(state.templates, key = { it.id }) { template ->
                val chosen = state.chosen == template.id
                Option(
                    badge = template.badge,
                    title = template.name(lang),
                    text = template.summary(lang),
                    chosen = chosen,
                    onClick = { viewModel.choose(template.id) }
                ) {
                    if (chosen) {
                        OutlinedButton(
                            onClick = { pickingDate = true },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                stringResource(
                                    R.string.setup_week1_starts,
                                    state.startDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                                ),
                                color = Purple
                            )
                        }
                    }
                }
            }
            item(key = TrainingState.EMPTY) {
                Option(
                    badge = "+",
                    title = stringResource(R.string.setup_empty_title),
                    text = stringResource(R.string.setup_empty_text),
                    chosen = state.chosen == TrainingState.EMPTY,
                    onClick = { viewModel.choose(TrainingState.EMPTY) }
                )
            }
        }
    }

    if (pickingDate) {
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
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

@Composable
private fun Option(
    badge: String,
    title: String,
    text: String,
    chosen: Boolean,
    onClick: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(2.dp, if (chosen) Purple else Color.Transparent), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Purple.copy(alpha = if (chosen) 1f else 0.12f))
        ) {
            Text(
                badge,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (chosen) Color.White else Purple
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra()
        }
    }
}
