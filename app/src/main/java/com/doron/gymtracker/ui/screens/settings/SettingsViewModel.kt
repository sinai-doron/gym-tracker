package com.doron.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.doron.gymtracker.data.BackupManager
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.SettingsPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SettingsPrefs,
    val healthConnect: HealthConnectManager,
    private val backupManager: BackupManager
) : ViewModel() {

    sealed interface BackupEvent {
        data object Saved : BackupEvent
        data object Restored : BackupEvent
        data class Failed(val reason: BackupManager.RestoreResult? = null) : BackupEvent
    }

    private val _backupEvent = MutableStateFlow<BackupEvent?>(null)
    val backupEvent: StateFlow<BackupEvent?> = _backupEvent.asStateFlow()

    fun exportBackup(target: Uri) {
        viewModelScope.launch {
            _backupEvent.value = runCatching { backupManager.export(target) }
                .fold({ BackupEvent.Saved }, { BackupEvent.Failed() })
        }
    }

    fun restoreBackup(source: Uri) {
        viewModelScope.launch {
            _backupEvent.value = runCatching { backupManager.restore(source) }.fold(
                { if (it == BackupManager.RestoreResult.Success) BackupEvent.Restored else BackupEvent.Failed(it) },
                { BackupEvent.Failed() }
            )
        }
    }

    fun backupEventHandled() {
        _backupEvent.value = null
    }

    val languageCode: StateFlow<String> = prefs.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "iw")

    private val _healthConnectGranted = MutableStateFlow(false)
    val healthConnectGranted: StateFlow<Boolean> = _healthConnectGranted.asStateFlow()

    init {
        refreshHealthConnectStatus()
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { prefs.setLanguageCode(code) }
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthConnectGranted.value = healthConnect.hasPermissions()
        }
    }
}
