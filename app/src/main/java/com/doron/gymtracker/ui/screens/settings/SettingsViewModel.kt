package com.doron.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import android.content.Context
import com.doron.gymtracker.account.Account
import com.doron.gymtracker.sync.SyncRepository
import com.doron.gymtracker.sync.SyncStatus
import com.doron.gymtracker.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.doron.gymtracker.account.Consent
import com.doron.gymtracker.account.SignedInUser
import com.doron.gymtracker.data.BackupManager
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.prefs.AudioFeedback
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
    private val backupManager: BackupManager,
    private val account: Account,
    private val sync: SyncRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val syncStatus: StateFlow<SyncStatus> = sync.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncStatus(null, null))

    fun syncNow() = SyncWorker.syncSoon(context)

    val user: StateFlow<SignedInUser?> = account.user
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val consent: StateFlow<Consent?> = account.consentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setSyncRoutes(routes: Boolean) {
        viewModelScope.launch {
            account.setRoutes(routes)
            // Turned off, the server deleted the route data; turned back on, it all goes up again.
            if (routes) sync.requeueRouteData()
            SyncWorker.syncSoon(context)
        }
    }

    fun setSyncMusic(music: Boolean) {
        viewModelScope.launch {
            account.setMusic(music)
            if (music) sync.requeueMusic()
            SyncWorker.syncSoon(context)
        }
    }

    fun signOut() {
        viewModelScope.launch { account.signOut() }
    }

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

    val lapDistanceM: StateFlow<Int> = prefs.lapDistanceM
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPrefs.DEFAULT_LAP_DISTANCE_M)

    val bodyWeightKg: StateFlow<Float?> = prefs.bodyWeightKg
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setBodyWeightKg(kg: Float?) {
        viewModelScope.launch { prefs.setBodyWeightKg(kg) }
    }

    val audioFeedback: StateFlow<AudioFeedback> = prefs.audioFeedback
        .stateIn(viewModelScope, SharingStarted.Eagerly, AudioFeedback.VOICE)

    fun setAudioFeedback(feedback: AudioFeedback) {
        viewModelScope.launch { prefs.setAudioFeedback(feedback) }
    }

    fun setLapDistanceM(meters: Int) {
        viewModelScope.launch { prefs.setLapDistanceM(meters) }
    }

    val stepGoal: StateFlow<Int> = prefs.stepGoal
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsPrefs.DEFAULT_STEP_GOAL)

    fun setStepGoal(steps: Int) {
        viewModelScope.launch { prefs.setStepGoal(steps) }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch { prefs.setLanguageCode(code) }
    }

    fun refreshHealthConnectStatus() {
        viewModelScope.launch {
            _healthConnectGranted.value = healthConnect.hasAllPermissions()
        }
    }
}
