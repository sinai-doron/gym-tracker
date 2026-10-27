package com.doron.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val healthConnect: HealthConnectManager
) : ViewModel() {

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
