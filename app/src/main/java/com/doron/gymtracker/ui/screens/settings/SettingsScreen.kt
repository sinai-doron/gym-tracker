package com.doron.gymtracker.ui.screens.settings

import android.app.LocaleManager
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.doron.gymtracker.R
import com.doron.gymtracker.data.BackupManager
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.AudioFeedback
import com.doron.gymtracker.data.prefs.SettingsPrefs

data class LanguageOption(val code: String, val displayName: String)

private val languages = listOf(
    LanguageOption("iw", "עברית"),
    LanguageOption("en", "English")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReviewPermissions: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.languageCode.collectAsState()
    val healthConnectGranted by viewModel.healthConnectGranted.collectAsState()
    val lapDistanceM by viewModel.lapDistanceM.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val bodyWeightKg by viewModel.bodyWeightKg.collectAsState()
    val audioFeedback by viewModel.audioFeedback.collectAsState()
    val context = LocalContext.current

    val backupEvent by viewModel.backupEvent.collectAsState()
    var pendingRestore by remember { mutableStateOf<android.net.Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> uri?.let { viewModel.exportBackup(it) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestore = uri }

    LaunchedEffect(backupEvent) {
        when (val event = backupEvent) {
            SettingsViewModel.BackupEvent.Saved ->
                Toast.makeText(context, R.string.backup_saved, Toast.LENGTH_SHORT).show()
            SettingsViewModel.BackupEvent.Restored -> {
                // The database was swapped underneath the running app: start fresh
                val restart = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(restart)
                Runtime.getRuntime().exit(0)
            }
            is SettingsViewModel.BackupEvent.Failed -> Toast.makeText(
                context,
                when (event.reason) {
                    BackupManager.RestoreResult.NotABackup -> R.string.backup_not_valid
                    BackupManager.RestoreResult.FromNewerVersion -> R.string.backup_newer_version
                    else -> R.string.backup_failed
                },
                Toast.LENGTH_LONG
            ).show()
            null -> Unit
        }
        if (backupEvent != null) viewModel.backupEventHandled()
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestore = null
                    viewModel.restoreBackup(uri)
                }) { Text(stringResource(R.string.restore_backup), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { viewModel.refreshHealthConnectStatus() }

    val lifecycleOwner = LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHealthConnectStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(
                    stringResource(R.string.language),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        languages.forEach { lang ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = currentLanguage == lang.code,
                                    onClick = {
                                        viewModel.setLanguage(lang.code)
                                        // Apply locale via API 33+ LocaleManager
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            context.getSystemService(LocaleManager::class.java)
                                                ?.applicationLocales = LocaleList.forLanguageTags(lang.code)
                                        }
                                    }
                                )
                                Text(
                                    lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.steps_settings), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        StepGoalDropdown(selected = stepGoal, onSelect = viewModel::setStepGoal)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.step_goal_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.running_settings), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        LapLengthDropdown(selectedM = lapDistanceM, onSelect = viewModel::setLapDistanceM)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.lap_length_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        AudioFeedbackDropdown(audioFeedback, onSelect = viewModel::setAudioFeedback)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.audio_feedback_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        BodyWeightField(bodyWeightKg, onChange = viewModel::setBodyWeightKg)
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.backup_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { exportLauncher.launch("gym-tracker-backup-${java.time.LocalDate.now()}.zip") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.save_backup))
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.restore_backup))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.permissions), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onReviewPermissions, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.permissions_review))
                }

                if (viewModel.healthConnect.isAvailable) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Health Connect",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (healthConnectGranted) {
                                Text(
                                    "הרשאות פעילות",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "אימונים נשמרים אוטומטית ב-Health Connect",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "סנכרן אימונים וצעדים עם Health Connect",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            hcPermissionLauncher.launch(HealthConnectManager.ALL_PERMISSIONS)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("אשר הרשאות")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LapLengthDropdown(selectedM: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = lapLengthLabel(selectedM),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.lap_length)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SettingsPrefs.LAP_DISTANCE_OPTIONS_M.forEach { meters ->
                DropdownMenuItem(
                    text = { Text(lapLengthLabel(meters)) },
                    onClick = {
                        onSelect(meters)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepGoalDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "%,d".format(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.step_goal)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SettingsPrefs.STEP_GOAL_OPTIONS.forEach { steps ->
                DropdownMenuItem(
                    text = { Text("%,d".format(steps)) },
                    onClick = {
                        onSelect(steps)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioFeedbackDropdown(selected: AudioFeedback, onSelect: (AudioFeedback) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = audioFeedbackLabel(selected),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(R.string.audio_feedback)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            AudioFeedback.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(audioFeedbackLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun audioFeedbackLabel(feedback: AudioFeedback): String = stringResource(
    when (feedback) {
        AudioFeedback.VOICE -> R.string.audio_feedback_voice
        AudioFeedback.TONES -> R.string.audio_feedback_tones
        AudioFeedback.OFF -> R.string.audio_feedback_off
    }
)

/** Saved as you type; anything that isn't a sensible weight clears it. */
@Composable
private fun BodyWeightField(weightKg: Float?, onChange: (Float?) -> Unit) {
    var text by remember(weightKg == null) { mutableStateOf(weightKg?.let(::formatWeight) ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter { it.isDigit() || it == '.' }.take(5)
            onChange(text.toFloatOrNull()?.takeIf { it in 25f..300f })
        },
        label = { Text(stringResource(R.string.body_weight)) },
        supportingText = { Text(stringResource(R.string.body_weight_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun formatWeight(kg: Float): String =
    if (kg == kg.toInt().toFloat()) kg.toInt().toString() else kg.toString()

@Composable
private fun lapLengthLabel(meters: Int): String = when {
    meters == 0 -> stringResource(R.string.laps_off)
    meters < 1000 -> stringResource(R.string.meters_value, meters)
    meters % 1000 == 0 -> stringResource(R.string.km_value, (meters / 1000).toString())
    else -> stringResource(R.string.km_value, "%.1f".format(meters / 1000.0))
}

