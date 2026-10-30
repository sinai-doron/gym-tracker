package com.doron.gymtracker.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.ui.theme.Purple
import com.doron.gymtracker.ui.theme.SuccessGreen

/** The kinds of activity, each in its color: green for cardio, purple for the gym (as in the history calendar). */
enum class Activity(@DrawableRes val icon: Int, val color: Color) {
    RUN(R.drawable.ic_activity_run, SuccessGreen),
    WALK(R.drawable.ic_activity_walk, SuccessGreen),
    LIFT(R.drawable.ic_activity_lift, Purple),
    /** Saved routes, which belong with running. */
    ROUTES(R.drawable.ic_routes, SuccessGreen)
}

/** An activity's icon, white on a circle of its color. */
@Composable
fun ActivityIcon(activity: Activity, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .background(activity.color, CircleShape)
    ) {
        Icon(
            painterResource(activity.icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}
