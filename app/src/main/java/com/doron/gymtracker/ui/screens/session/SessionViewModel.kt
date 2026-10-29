package com.doron.gymtracker.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doron.gymtracker.data.repository.SessionRepository
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.domain.model.ExerciseEntry
import com.doron.gymtracker.domain.model.SetRecord
import com.doron.gymtracker.domain.model.Workout
import com.doron.gymtracker.domain.model.TrackLog
import com.doron.gymtracker.domain.usecase.FinishSessionUseCase
import com.doron.gymtracker.domain.usecase.IsDeloadWeekUseCase
import com.doron.gymtracker.domain.usecase.ResolveExerciseVariantUseCase
import com.doron.gymtracker.media.NowPlayingProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val workout: Workout? = null,
    val elapsedSecs: Long = 0L,
    val entries: Map<String, ExerciseEntry> = emptyMap(),
    val isFinished: Boolean = false,
    val isDeloadWeek: Boolean = false,
    val tracks: List<TrackLog> = emptyList(),
    val startedAt: Long = 0L,
    /** Sets already logged in this session (non-empty when resuming), by exercise id. */
    val loggedSets: Map<String, List<SetRecord>> = emptyMap()
)

class SessionViewModel @AssistedInject constructor(
    @Assisted val sessionId: Long,
    @Assisted val workoutId: String,
    private val sessionRepo: SessionRepository,
    private val workoutRepo: WorkoutRepository,
    private val finishSessionUseCase: FinishSessionUseCase,
    private val resolveVariant: ResolveExerciseVariantUseCase,
    private val isDeloadWeek: IsDeloadWeekUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sessionId: Long, workoutId: String): SessionViewModel
    }


    val restTimer = RestTimer(viewModelScope)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isDeloadWeek = isDeloadWeek()) }
        viewModelScope.launch {
            // A resumed session keeps its original start time and the sets already logged
            val startedAt = sessionRepo.getById(sessionId)?.startedAt ?: System.currentTimeMillis()
            val loggedSets = sessionRepo.getEntriesForSession(sessionId).associate { it.exerciseId to it.sets }
            val raw = workoutRepo.getById(workoutId)
            // Resolve A/B variant by the week the session started in
            val resolved = raw?.let { w ->
                w.copy(exercises = w.exercises.map { resolveVariant(it, startedAt) })
            }
            // Loaded together so exercise sheets are created with their logged sets
            _uiState.update { it.copy(workout = resolved, startedAt = startedAt, loggedSets = loggedSets) }
            while (!_uiState.value.isFinished) {
                _uiState.update { it.copy(elapsedSecs = (System.currentTimeMillis() - startedAt) / 1000) }
                delay(1_000)
            }
        }
        viewModelScope.launch {
            var lastTrackKey = ""
            NowPlayingProvider.state.collect { nowPlaying ->
                if (_uiState.value.isFinished) return@collect
                if (nowPlaying == null || !nowPlaying.isPlaying) return@collect
                val key = "${nowPlaying.title}|${nowPlaying.artist}"
                if (key != lastTrackKey && nowPlaying.title.isNotBlank()) {
                    lastTrackKey = key
                    sessionRepo.logTrack(sessionId, nowPlaying.title, nowPlaying.artist)
                    val tracks = sessionRepo.getTracksForSession(sessionId)
                    _uiState.update { it.copy(tracks = tracks) }
                }
            }
        }
    }

    fun finishSession() {
        viewModelScope.launch {
            restTimer.dismiss()
            finishSessionUseCase(sessionId)
            _uiState.update { it.copy(isFinished = true) }
        }
    }

    /** Deletes a session started by mistake, with everything logged in it. */
    fun discardSession(onDiscarded: () -> Unit) {
        restTimer.dismiss()
        viewModelScope.launch {
            sessionRepo.deleteSession(sessionId)
            onDiscarded()
        }
    }

    fun onEntryUpdated(entry: ExerciseEntry) {
        _uiState.update { state ->
            state.copy(entries = state.entries + (entry.exerciseId to entry))
        }
    }

    /** Saves a new set count to the training plan, so it sticks for future sessions too. */
    fun updateTargetSets(exerciseId: String, sets: Int) {
        viewModelScope.launch {
            workoutRepo.updateTargetSets(exerciseId, sets)
        }
    }

    fun deleteSet(setId: Long) {
        viewModelScope.launch {
            sessionRepo.deleteSetRecord(setId)
        }
    }

    fun deleteExerciseFromSession(exerciseId: String) {
        viewModelScope.launch {
            sessionRepo.deleteExerciseFromSession(sessionId, exerciseId)
        }
    }
}
