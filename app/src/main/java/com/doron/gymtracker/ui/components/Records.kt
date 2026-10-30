package com.doron.gymtracker.ui.components

import com.doron.gymtracker.domain.model.PersonalRecord
import kotlin.math.roundToLong

/** A record's value as it reads: "4:51" or "1:02:13" for times, "5.29 ק״מ" for distances. */
fun PersonalRecord.valueText(): String =
    if (kind.isTime) {
        val total = value.roundToLong()
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    } else "%.2f ק״מ".format(value / 1000)
