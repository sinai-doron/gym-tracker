package com.doron.gymtracker.domain.model

sealed interface ProgressionHint {
    // Every set at the top weight reached the top of the range last time: add the equipment's step
    data class Increase(val addKg: Float) : ProgressionHint
    // Same weight, no rep improvement across 3 sessions
    object Stalled : ProgressionHint
    // Neither condition met
    object OnTrack : ProgressionHint
}
