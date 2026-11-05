package com.doron.gymtracker.ui.screens.log

import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.HealthConnectManager
import com.doron.gymtracker.data.db.ActivityTypeMigration
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.toMeters
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.TrainingTypes
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.usecase.GetNextWorkoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LogSessionState(
    val types: List<TrainingType> = emptyList(),
    val typeId: String = ActivityTypeMigration.STRENGTH,
    /** The active programme's workouts, for a strength session. */
    val workouts: List<Workout> = emptyList(),
    val workoutId: String? = null,
    val date: LocalDate = LocalDate.now(),
    val start: LocalTime = LocalTime.now().minusHours(1).withSecond(0).withNano(0),
    val hours: String = "1",
    val minutes: String = "0",
    /** In km. */
    val distance: String = "",
    val calories: String = "",
    val note: String = "",
    val saveToHealthConnect: Boolean = false,
    val healthConnectAvailable: Boolean = false,
    val bodyWeightKg: Float? = null,
    val saving: Boolean = false,
) {
    val type: TrainingType? get() = types.firstOrNull { it.id == typeId }
    val isStrength: Boolean get() = type?.recorder == Recorder.EXERCISES

    val durationS: Int get() = (hours.toIntOrNull() ?: 0) * 3600 + (minutes.toIntOrNull() ?: 0) * 60

    fun startMs(zone: ZoneId = ZoneId.systemDefault()): Long = date.atTime(start).atZone(zone).toInstant().toEpochMilli()

    /** What an empty calories field is saved as: the type's MET × body weight × hours. */
    val estimatedCalories: Int? get() = type?.estimateCalories(durationS.toLong(), bodyWeightKg)

    /** Why it can't be saved yet, or null when it can. */
    fun problem(now: Long = System.currentTimeMillis()): LogProblem? = when {
        type == null -> LogProblem.NO_TYPE
        durationS <= 0 -> LogProblem.NO_DURATION
        startMs() + durationS * 1000L > now -> LogProblem.IN_THE_FUTURE
        isStrength && workoutId == null -> LogProblem.NO_WORKOUT
        else -> null
    }
}

enum class LogProblem { NO_TYPE, NO_DURATION, IN_THE_FUTURE, NO_WORKOUT }

@HiltViewModel
class LogSessionViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val types: TrainingTypes,
    private val workouts: WorkoutRepository,
    private val nextWorkout: GetNextWorkoutUseCase,
    private val settings: SettingsPrefs,
    private val healthConnect: HealthConnectManager
) : ViewModel() {

    private val _state = MutableStateFlow(LogSessionState())
    val state: StateFlow<LogSessionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hc = healthConnect.isAvailable && healthConnect.hasPermissions()
            _state.update {
                it.copy(
                    types = types.shown(),
                    workouts = workouts.getAll(),
                    workoutId = nextWorkout()?.id,
                    bodyWeightKg = settings.bodyWeightKg.first(),
                    healthConnectAvailable = hc,
                    saveToHealthConnect = hc
                )
            }
        }
    }

    fun update(change: (LogSessionState) -> LogSessionState) = _state.update(change)

    /**
     * Saves the session as logged by hand. A strength session then goes to [onStrength], to fill in
     * its sets; anything else to [onDone]. [title] names it in Health Connect.
     */
    fun save(title: String, onStrength: (sessionId: Long, workoutId: String) -> Unit, onDone: () -> Unit) {
        val s = _state.value
        val type = s.type ?: return
        if (s.problem() != null || s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val startedAt = s.startMs()
            val finishedAt = startedAt + s.durationS * 1000L
            val calories = s.calories.toIntOrNull() ?: s.estimatedCalories
            // Typed for another type before switching: only a type with a distance keeps it
            // Typed in km or miles, as the runner reads distances
            val unit = settings.profile.first().distanceUnit
            val distanceMeters = if (!type.hasDistance) null else s.distance.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let { unit.toMeters(it) }
            val sessionType = when {
                s.isStrength -> "GYM"
                type.id == ActivityTypeMigration.RUN -> "RUNNING"
                type.id == ActivityTypeMigration.WALK -> "WALKING"
                else -> "OTHER"
            }
            val workoutId = if (s.isStrength) s.workoutId!! else sessionType
            val id = sessions.logSession(sessionType, workoutId, type.id, startedAt, s.durationS, distanceMeters, calories, s.note)
            if (s.saveToHealthConnect) {
                if (s.isStrength) {
                    healthConnect.writeWorkoutSession(startedAt, finishedAt, title)
                } else {
                    healthConnect.writeOtherSession(
                        startedAt, finishedAt, title,
                        exerciseType = type.healthConnectType ?: ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
                        calories = calories,
                        distanceMeters = distanceMeters,
                        manual = true
                    )
                }
            }
            if (s.isStrength) onStrength(id, workoutId) else onDone()
        }
    }
}
