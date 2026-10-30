package com.doron.gymtracker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.domain.model.BadgeStatus
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.usecase.GetPersonalRecordsUseCase
import com.doron.gymtracker.domain.usecase.GetRunBadgesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class BadgesViewModel @Inject constructor(
    private val getRunBadges: GetRunBadgesUseCase,
    private val getPersonalRecords: GetPersonalRecordsUseCase
) : ViewModel() {

    private val _badges = MutableStateFlow<List<BadgeStatus>>(emptyList())
    val badges: StateFlow<List<BadgeStatus>> = _badges.asStateFlow()

    private val _records = MutableStateFlow<List<PersonalRecord>>(emptyList())
    val records: StateFlow<List<PersonalRecord>> = _records.asStateFlow()

    fun refresh() {
        viewModelScope.launch { _badges.value = getRunBadges() }
        viewModelScope.launch { _records.value = getPersonalRecords() }
    }
}
