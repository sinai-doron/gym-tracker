package com.doron.gymtracker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.prefs.SettingsPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: SettingsPrefs
) : ViewModel() {

    val languageCode: StateFlow<String> = prefs.languageCode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "iw")

    fun setLanguage(code: String) {
        viewModelScope.launch { prefs.setLanguageCode(code) }
    }
}
