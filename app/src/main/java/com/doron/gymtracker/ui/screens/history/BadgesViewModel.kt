package com.doron.gymtracker.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.domain.model.BadgeStatus
import com.doron.gymtracker.domain.usecase.GetRunBadgesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val getRunBadges: GetRunBadgesUseCase
) : ViewModel() {

    private val _badges = MutableStateFlow<List<BadgeStatus>>(emptyList())
    val badges: StateFlow<List<BadgeStatus>> = _badges.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _badges.value = getRunBadges() }
    }
}
