package com.doron.gymtracker.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.prefs.Profile
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.prefs.WeekStart
import com.doron.gymtracker.data.prefs.WeightUnit
import com.doron.gymtracker.data.repository.ProgramRepository
import com.doron.gymtracker.data.templates.ProgramTemplate
import com.doron.gymtracker.data.templates.ProgramTemplates
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** "About you": the profile and the two details Settings already had, body weight and steps goal. */
data class AboutYouState(
    val profile: Profile,
    /** As typed, in the chosen weight unit. */
    val bodyWeight: String,
    val stepGoal: String,
    val birthYear: String,
)

@HiltViewModel
class AboutYouViewModel @Inject constructor(
    private val settings: SettingsPrefs
) : ViewModel() {

    private val _state = MutableStateFlow<AboutYouState?>(null)
    val state: StateFlow<AboutYouState?> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = settings.profile.first()
            _state.value = AboutYouState(
                profile = profile,
                bodyWeight = settings.bodyWeightKg.first()?.let { formatWeight(it, profile.weightUnit) }.orEmpty(),
                stepGoal = settings.stepGoal.first().toString(),
                birthYear = profile.birthYear?.toString().orEmpty(),
            )
        }
    }

    fun update(change: (AboutYouState) -> AboutYouState) = _state.update { it?.let(change) }

    /** Switching units converts what was typed, so the weight stays the same weight. */
    fun setWeightUnit(unit: WeightUnit) = update { s ->
        val kg = parseWeightKg(s.bodyWeight, s.profile.weightUnit)
        s.copy(profile = s.profile.copy(weightUnit = unit), bodyWeight = kg?.let { formatWeight(it, unit) } ?: s.bodyWeight)
    }

    /** Saves what makes sense and leaves out what doesn't: a body weight of 3, a birth year of 1066. */
    fun save(thisYear: Int, onDone: () -> Unit) {
        val s = _state.value ?: return
        viewModelScope.launch {
            settings.setProfile(
                s.profile.copy(birthYear = s.birthYear.toIntOrNull()?.takeIf { it in Profile.birthYears(thisYear) })
            )
            settings.setBodyWeightKg(parseWeightKg(s.bodyWeight, s.profile.weightUnit)?.takeIf { it in 25f..300f })
            s.stepGoal.toIntOrNull()?.takeIf { it in 1_000..100_000 }?.let { settings.setStepGoal(it) }
            onDone()
        }
    }

    companion object {
        private const val KG_PER_LB = 0.45359237f

        fun parseWeightKg(text: String, unit: WeightUnit): Float? =
            text.replace(',', '.').toFloatOrNull()?.let { if (unit == WeightUnit.LB) it * KG_PER_LB else it }

        fun formatWeight(kg: Float, unit: WeightUnit): String {
            val value = if (unit == WeightUnit.LB) kg / KG_PER_LB else kg
            val tenths = (value * 10).roundToInt()
            return if (tenths % 10 == 0) (tenths / 10).toString() else "%.1f".format(java.util.Locale.ROOT, tenths / 10f)
        }
    }
}

/** "Your training": a template, or an empty programme. */
data class TrainingState(
    val templates: List<ProgramTemplate> = emptyList(),
    /** A template's id, [EMPTY], or null before a choice. */
    val chosen: String? = null,
    val startDate: LocalDate = LocalDate.now(),
    val saving: Boolean = false,
    /** There is a programme already (a restored backup, the owner's A/B/C): first launch skips this step. */
    val hasProgramme: Boolean = false,
) {
    companion object {
        const val EMPTY = "empty"
    }
}

@HiltViewModel
class TrainingViewModel @Inject constructor(
    private val templates: ProgramTemplates,
    private val programs: ProgramRepository,
    private val settings: SettingsPrefs
) : ViewModel() {

    private val _state = MutableStateFlow(TrainingState())
    val state: StateFlow<TrainingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val weekStart = settings.profile.first().weekStart
            val hasProgramme = programs.getAll().any { it.isActive }
            _state.update {
                it.copy(templates = templates.all(), startDate = weekStartOf(LocalDate.now(), weekStart), hasProgramme = hasProgramme)
            }
        }
    }

    fun choose(id: String) = _state.update { it.copy(chosen = id) }

    fun setStartDate(date: LocalDate) = _state.update { it.copy(startDate = date) }

    /**
     * Writes the chosen programme. A template's is ready to follow, so it becomes the active one
     * and goes to [onTemplate]; an empty one goes to [onEmpty], to fill in the builder, named
     * [emptyName].
     */
    fun create(lang: String, emptyName: String, onTemplate: (String) -> Unit, onEmpty: (String) -> Unit) {
        val s = _state.value
        val chosen = s.chosen ?: return
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            if (chosen == TrainingState.EMPTY) {
                // Active straight away only for someone with no programme yet; anyone else keeps
                // following theirs while they fill the new one, and makes it active from the list
                val first = programs.getAll().none { it.isActive }
                val id = programs.create(emptyName, s.startDate)
                if (first) programs.makeActive(id)
                onEmpty(id)
            } else {
                val template = s.templates.first { it.id == chosen }
                onTemplate(templates.install(template, lang, s.startDate))
            }
        }
    }

    companion object {
        /** The first day of [date]'s week: week 1 of a new programme starts there. */
        fun weekStartOf(date: LocalDate, weekStart: WeekStart): LocalDate =
            date.with(TemporalAdjusters.previousOrSame(if (weekStart == WeekStart.MONDAY) DayOfWeek.MONDAY else DayOfWeek.SUNDAY))
    }
}
