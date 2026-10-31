package com.doron.gymtracker.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.doron.gymtracker.R

/** Families of achievements beyond the distance badges; each has bronze, silver and gold. */
enum class AchievementFamily(@StringRes val title: Int, @StringRes val explain: Int) {
    CLIMBER(R.string.achievement_family_climber, R.string.achievement_family_climber_explain),
    MILEAGE(R.string.achievement_family_mileage, R.string.achievement_family_mileage_explain),
    IRON(R.string.achievement_family_iron, R.string.achievement_family_iron_explain),
    REGULAR(R.string.achievement_family_regular, R.string.achievement_family_regular_explain),
    SPECIAL(R.string.achievement_family_special, R.string.achievement_family_special_explain),
}

enum class Tier { BRONZE, SILVER, GOLD, SPECIAL }

/**
 * Every achievement, its family and tier, and the [target] it is earned at, in the family's unit:
 * meters climbed in one run or walk, kilometers in all, tonnes lifted, weeks in a row, or 1 for a
 * one-off. [badge] is the short text on the badge, for accessibility; [image] its artwork, in the
 * style of the distance badges (bronze, silver, gold; the one-offs in teal).
 */
enum class AchievementKind(
    val family: AchievementFamily,
    val tier: Tier,
    val target: Double,
    val badge: String,
    @StringRes val title: Int,
    @DrawableRes val image: Int,
) {
    CLIMB_50(AchievementFamily.CLIMBER, Tier.BRONZE, 50.0, "50m", R.string.achievement_climb_50, R.drawable.badge_climb_50),
    CLIMB_150(AchievementFamily.CLIMBER, Tier.SILVER, 150.0, "150m", R.string.achievement_climb_150, R.drawable.badge_climb_150),
    CLIMB_300(AchievementFamily.CLIMBER, Tier.GOLD, 300.0, "300m", R.string.achievement_climb_300, R.drawable.badge_climb_300),
    MILES_25(AchievementFamily.MILEAGE, Tier.BRONZE, 25.0, "25", R.string.achievement_miles_25, R.drawable.badge_miles_25),
    MILES_100(AchievementFamily.MILEAGE, Tier.SILVER, 100.0, "100", R.string.achievement_miles_100, R.drawable.badge_miles_100),
    MILES_500(AchievementFamily.MILEAGE, Tier.GOLD, 500.0, "500", R.string.achievement_miles_500, R.drawable.badge_miles_500),
    IRON_10T(AchievementFamily.IRON, Tier.BRONZE, 10.0, "10t", R.string.achievement_iron_10, R.drawable.badge_iron_10t),
    IRON_50T(AchievementFamily.IRON, Tier.SILVER, 50.0, "50t", R.string.achievement_iron_50, R.drawable.badge_iron_50t),
    IRON_100T(AchievementFamily.IRON, Tier.GOLD, 100.0, "100t", R.string.achievement_iron_100, R.drawable.badge_iron_100t),
    REGULAR_2(AchievementFamily.REGULAR, Tier.BRONZE, 2.0, "2", R.string.achievement_regular_2, R.drawable.badge_regular_2),
    REGULAR_4(AchievementFamily.REGULAR, Tier.SILVER, 4.0, "4", R.string.achievement_regular_4, R.drawable.badge_regular_4),
    REGULAR_8(AchievementFamily.REGULAR, Tier.GOLD, 8.0, "8", R.string.achievement_regular_8, R.drawable.badge_regular_8),
    ROTATION(AchievementFamily.SPECIAL, Tier.SPECIAL, 1.0, "ABC", R.string.achievement_rotation, R.drawable.badge_rotation),
    NIGHT_OWL(AchievementFamily.SPECIAL, Tier.SPECIAL, 1.0, "🦉", R.string.achievement_night_owl, R.drawable.badge_night_owl),
    EARLY_BIRD(AchievementFamily.SPECIAL, Tier.SPECIAL, 1.0, "🐦", R.string.achievement_early_bird, R.drawable.badge_early_bird),
}

/**
 * Where one achievement stands: when it was earned and by which session (null until it is), and
 * [progress] towards its target in the same unit (the best or the total so far).
 */
data class AchievementStatus(
    val kind: AchievementKind,
    val earnedAt: Long? = null,
    val sessionId: Long? = null,
    val progress: Double = 0.0,
) {
    val isEarned: Boolean get() = earnedAt != null
    val fraction: Float get() = (progress / kind.target).coerceIn(0.0, 1.0).toFloat()
}
