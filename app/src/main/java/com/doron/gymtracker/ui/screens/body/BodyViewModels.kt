package com.doron.gymtracker.ui.screens.body

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.prefs.BodyPrefs
import com.doron.gymtracker.data.prefs.WaterReminder
import com.doron.gymtracker.data.prefs.WeightReminder
import com.doron.gymtracker.data.repository.WaterRepository
import com.doron.gymtracker.data.repository.WeightRepository
import com.doron.gymtracker.domain.model.Drink
import com.doron.gymtracker.domain.model.Reminders
import com.doron.gymtracker.domain.model.WaterGoal
import com.doron.gymtracker.domain.model.WeighIn
import com.doron.gymtracker.domain.model.WeightGoal
import com.doron.gymtracker.service.BodyReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class WeightViewModel @Inject constructor(repo: WeightRepository) : ViewModel() {
    /** Newest first; null until read. */
    val entries: StateFlow<List<WeighIn>?> = repo.entries.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val goal: StateFlow<WeightGoal?> = repo.goal.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

@HiltViewModel
class WeightLogViewModel @Inject constructor(
    private val repo: WeightRepository,
    saved: SavedStateHandle
) : ViewModel() {
    /** The weigh-in being changed; null for a new one. */
    val editingId: Long? = saved.get<Long>("id")?.takeIf { it > 0 }

    data class Start(val kg: Double, val measuredAt: Long, val lastKg: Double?)

    private val _start = MutableStateFlow<Start?>(null)
    /** Where the scale starts: the weigh-in being changed, else the last one (75 kg with none yet). */
    val start: StateFlow<Start?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val editing = editingId?.let { repo.get(it) }
            val last = repo.latestKg()
            _start.value = Start(editing?.kg ?: last ?: 75.0, editing?.measuredAt ?: System.currentTimeMillis(), last)
        }
    }

    fun save(kg: Double, measuredAt: Long, onSaved: () -> Unit) {
        viewModelScope.launch {
            if (editingId != null) repo.update(editingId, measuredAt, kg) else repo.log(measuredAt, kg)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = editingId ?: return
        viewModelScope.launch { repo.delete(id); onDeleted() }
    }
}

@HiltViewModel
class WeightGoalViewModel @Inject constructor(private val repo: WeightRepository) : ViewModel() {
    data class Start(val currentKg: Double, val goal: WeightGoal?)

    private val _start = MutableStateFlow<Start?>(null)
    val start: StateFlow<Start?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            val current = repo.latestKg() ?: 75.0
            repo.goal.collect { goal -> if (_start.value == null) _start.value = Start(current, goal) }
        }
    }

    /** A new goal starts from today's weight; changing one keeps where it started. */
    fun save(targetKg: Double, pace: Double, onSaved: () -> Unit) {
        val s = _start.value ?: return
        viewModelScope.launch {
            val goal = s.goal
            repo.setGoal(
                WeightGoal(
                    startKg = goal?.startKg ?: s.currentKg,
                    startAt = goal?.startAt ?: System.currentTimeMillis(),
                    targetKg = targetKg,
                    paceKgPerWeek = pace
                )
            )
            onSaved()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WaterViewModel @Inject constructor(
    private val repo: WaterRepository,
    prefs: BodyPrefs
) : ViewModel() {
    private val zone = java.time.ZoneId.systemDefault()

    /** The day changes under an open screen: today's start, read again each minute. */
    private val today = flow {
        while (true) {
            emit(LocalDate.now())
            delay(60_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    /** The last 7 days' drinks, today's included, newest first. */
    val week: StateFlow<List<Drink>?> = today
        .flatMapLatest { day -> repo.drinksSince(Reminders.startOfDay(day.minusDays(6), zone)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val goal: StateFlow<WaterGoal> = repo.goal.stateIn(viewModelScope, SharingStarted.Eagerly, WaterGoal())
    val reminder: StateFlow<WaterReminder> = prefs.waterReminder.stateIn(viewModelScope, SharingStarted.Eagerly, WaterReminder())

    fun add(ml: Int) {
        viewModelScope.launch { repo.add(System.currentTimeMillis(), ml) }
    }

    fun remove(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}

@HiltViewModel
class WaterGoalViewModel @Inject constructor(
    private val repo: WaterRepository,
    weight: WeightRepository
) : ViewModel() {
    data class Start(val goal: WaterGoal, val weightKg: Double?)

    private val _start = MutableStateFlow<Start?>(null)
    val start: StateFlow<Start?> = _start.asStateFlow()

    init {
        viewModelScope.launch { _start.value = Start(repo.goalNow(), weight.latestKg()) }
    }

    fun save(goal: WaterGoal, onSaved: () -> Unit) {
        viewModelScope.launch { repo.setGoal(goal); onSaved() }
    }
}

/** The Settings section for weight and water: their Home cards and reminders. */
@HiltViewModel
class BodySettingsViewModel @Inject constructor(
    private val prefs: BodyPrefs,
    @ApplicationContext private val context: Context
) : ViewModel() {
    val showWeight: StateFlow<Boolean> = prefs.showWeightCard.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val showWater: StateFlow<Boolean> = prefs.showWaterCard.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val weightReminder: StateFlow<WeightReminder> = prefs.weightReminder.stateIn(viewModelScope, SharingStarted.Eagerly, WeightReminder())
    val waterReminder: StateFlow<WaterReminder> = prefs.waterReminder.stateIn(viewModelScope, SharingStarted.Eagerly, WaterReminder())

    fun setShowWeight(show: Boolean) { viewModelScope.launch { prefs.setShowWeightCard(show) } }
    fun setShowWater(show: Boolean) { viewModelScope.launch { prefs.setShowWaterCard(show) } }

    fun setWeightReminder(r: WeightReminder) {
        viewModelScope.launch {
            prefs.setWeightReminder(r)
            BodyReminderWorker.schedule(context, prefs, BodyReminderWorker.WEIGHT)
        }
    }

    fun setWaterReminder(r: WaterReminder) {
        viewModelScope.launch {
            prefs.setWaterReminder(r)
            BodyReminderWorker.schedule(context, prefs, BodyReminderWorker.WATER)
        }
    }
}
