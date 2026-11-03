package com.doron.gymtracker.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doron.gymtracker.R
import com.doron.gymtracker.domain.model.Recorder
import com.doron.gymtracker.domain.model.TrainingType

/** The icons a training type can have, by the slug stored with it. */
object TrainingTypeIcons {
    val EMOJI: Map<String, String> = linkedMapOf(
        "dumbbell" to "🏋️",
        "run" to "🏃",
        "walk" to "🚶",
        "pilates" to "🤸",
        "yoga" to "🧘",
        "mobility" to "🙆",
        "martial_arts" to "🥋",
        "boxing" to "🥊",
        "swimming" to "🏊",
        "hiit" to "🔥",
        "football" to "⚽",
        "basketball" to "🏀",
        "tennis" to "🎾",
        "volleyball" to "🏐",
        "cycling" to "🚴",
        "climbing" to "🧗",
        "dance" to "💃",
        "surfing" to "🏄",
        "skiing" to "⛷️",
        "golf" to "⛳",
        "star" to "⭐",
    )

    /** What a user picks from for a type of their own. */
    val CHOICES: List<String> = EMOJI.keys.filter { it !in setOf("dumbbell", "run", "walk") }

    fun emoji(slug: String): String = EMOJI[slug] ?: "⭐"
}

/** A type's name: the user's, or a built-in's in the app's language. */
@Composable
fun TrainingType.displayName(): String = name ?: stringResource(builtinTypeName(id))

/** A built-in type's name, by its id; anything else is "Other". */
@StringRes
fun builtinTypeName(id: String): Int = when (id) {
    "strength" -> R.string.type_strength
    "run" -> R.string.type_run
    "walk" -> R.string.type_walk
    "pilates" -> R.string.type_pilates
    "yoga" -> R.string.type_yoga
    "martial_arts" -> R.string.type_martial_arts
    "swimming" -> R.string.type_swimming
    "hiit" -> R.string.type_hiit
    "team_sports" -> R.string.type_team_sports
    "mobility" -> R.string.type_mobility
    "cardio_machines" -> R.string.type_cardio_machines
    else -> R.string.type_other
}

/** How a type records, in a few words. */
@Composable
fun TrainingType.recorderLabel(): String = stringResource(
    when (recorder) {
        Recorder.EXERCISES -> R.string.recorder_exercises
        Recorder.GPS -> R.string.recorder_gps
        Recorder.DURATION -> R.string.recorder_timer
    }
)

/** The type's "#RRGGBB" as a colour; grey if it isn't one. */
fun TrainingType.colorValue(): Color =
    color.removePrefix("#").takeIf { it.length == 6 }?.toLongOrNull(16)?.let { Color(0xFF000000 or it) } ?: Color.Gray

/** A type's icon on a circle of its colour. */
@Composable
fun TrainingTypeBadge(type: TrainingType, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(type.colorValue().copy(alpha = 0.18f), CircleShape)
    ) {
        Text(TrainingTypeIcons.emoji(type.icon), fontSize = (size.value * 0.5f).sp)
    }
}

/**
 * The colours to fill a day's circle with, one per colour among [types] (a run and a walk share
 * one): strength last, so it takes the right-hand side as it always has.
 */
fun dayColors(types: List<TrainingType>): List<Color> =
    types.sortedByDescending { it.sortOrder }.map { it.colorValue() }.distinct()

/** Fills the circle with [colors] side by side, or with [empty] when there are none. */
fun DrawScope.drawDay(colors: List<Color>, empty: Color) {
    if (colors.isEmpty()) {
        drawRect(empty)
        return
    }
    val slice = size.width / colors.size
    colors.forEachIndexed { i, color ->
        drawRect(color, topLeft = Offset(i * slice, 0f), size = Size(slice, size.height))
    }
}
