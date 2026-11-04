package com.doron.gymtracker

import java.util.Locale

/**
 * The language the app shows: the one chosen in Settings (a per-app locale), else the phone's.
 * Android makes it the process's default locale, so code outside the UI reads it here; the UI
 * reads it from its configuration (ui/components/AppLanguage.kt), so it redraws on a change.
 */
object AppLocale {
    fun current(): Locale = Locale.getDefault()

    /** A locale's language as the exercise catalogue and the map tiles name it: "he" (Android's old "iw" too), "en". */
    fun language(locale: Locale = current()): String = locale.language.let { if (it == "iw") "he" else it }
}
