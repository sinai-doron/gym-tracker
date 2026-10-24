package com.doron.gymtracker.domain.model

sealed interface ProgressionHint {
    // All working sets hit top of rep range for 3 sessions in a row
    data class Increase(val addKg: Float) : ProgressionHint
    // Same weight, no rep improvement across 3 sessions
    object Stalled : ProgressionHint
    // Neither condition met
    object OnTrack : ProgressionHint
}
