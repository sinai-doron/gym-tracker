package com.doron.gymtracker.domain.model

/** Earned by the first run that reaches [meters]. */
enum class RunBadge(val meters: Int, val label: String) {
    ONE_K(1_000, "1K"),
    FIVE_K(5_000, "5K"),
    TEN_K(10_000, "10K")
}

/** A badge and the run that first earned it, if any has. */
data class BadgeStatus(val badge: RunBadge, val earnedAt: Long? = null, val sessionId: Long? = null) {
    val isEarned: Boolean get() = sessionId != null
}
