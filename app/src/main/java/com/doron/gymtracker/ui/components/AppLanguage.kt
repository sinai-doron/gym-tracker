package com.doron.gymtracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.doron.gymtracker.AppLocale
import com.doron.gymtracker.domain.model.ExerciseDefinition
import com.doron.gymtracker.domain.model.nameIn
import java.util.Locale

/** The locale the app shows, for dates and numbers; redraws when the language changes. */
@Composable
fun appLocale(): Locale = LocalConfiguration.current.locales[0]

/** The app's language as the catalogue names it: "he", "en". */
@Composable
fun appLanguage(): String = AppLocale.language(appLocale())

/** The exercise's name in the app's language: the library's, else the one the user gave it. */
@Composable
fun ExerciseDefinition.localizedName(): String = nameIn(appLanguage())
