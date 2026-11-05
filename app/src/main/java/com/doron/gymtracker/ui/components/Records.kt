package com.doron.gymtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.doron.gymtracker.R
import com.doron.gymtracker.data.prefs.DistanceUnit
import com.doron.gymtracker.domain.model.PersonalRecord
import com.doron.gymtracker.domain.model.RecordKind
import kotlin.math.roundToLong

/**
 * A record kind's name: "1 km" for the fastest ones, else what it is. Those are race distances,
 * which stay in kilometres for a runner in miles too, as a 5K does.
 */
@Composable
fun RecordKind.label(): String = when (this) {
    RecordKind.FASTEST_1K, RecordKind.FASTEST_5K, RecordKind.FASTEST_10K -> lengthText(meters!!, DistanceUnit.KM)
    RecordKind.LONGEST_RUN -> stringResource(R.string.home_record_longest_run)
    RecordKind.BEST_WEEK -> stringResource(R.string.home_record_best_week)
}

/** A record's value as it reads: "4:51" or "1:02:13" for times, "5.29 km" (or miles) for distances. */
@Composable
fun PersonalRecord.valueText(): String =
    if (kind.isTime) {
        val total = value.roundToLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    } else distanceText(value)
