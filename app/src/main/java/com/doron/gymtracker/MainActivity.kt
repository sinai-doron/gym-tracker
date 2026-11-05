package com.doron.gymtracker

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import com.doron.gymtracker.sync.SyncWorker
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.CardioRun
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.ExerciseLibrary
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.ui.components.LocalDistanceUnit
import com.doron.gymtracker.ui.navigation.NavGraph
import com.doron.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var exerciseLibrary: ExerciseLibrary
    @Inject lateinit var cardioRecorder: CardioRecorder
    @Inject lateinit var settingsPrefs: SettingsPrefs

    // A run still going (or cut off by the system) to reopen, found at launch
    private val runToResume = mutableStateOf<CardioRun?>(null)

    // Null until read, so neither home nor the wizard flashes up first
    private val showOnboarding = mutableStateOf<Boolean?>(null)

    // The splash stays until the first screen has its data, or 3 s at most
    @Volatile private var firstScreenReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashUntil = System.currentTimeMillis() + 3_000
        installSplashScreen().setKeepOnScreenCondition {
            !firstScreenReady && System.currentTimeMillis() < splashUntil
        }
        super.onCreate(savedInstanceState)
        if (applyDefaultLocale()) return  // recreated in Hebrew straight away
        enableEdgeToEdge()
        lifecycleScope.launch {
            workoutRepository.ensureSeeded()
            // After the plan: a fresh install's plan rows must exist to be linked to the library
            exerciseLibrary.ensureSeeded()
        }
        lifecycleScope.launch {
            if (!settingsPrefs.onboardingDone.first() && isUpdate()) {
                // Upgraded from a version without the wizard: permissions were already asked for
                settingsPrefs.setOnboardingDone()
            }
            showOnboarding.value = !settingsPrefs.onboardingDone.first()
        }
        if (savedInstanceState == null) {
            lifecycleScope.launch { runToResume.value = cardioRecorder.recoverOpenRun() }
        }
        setContent {
            val distanceUnit by remember { settingsPrefs.profile.map { it.distanceUnit } }
                .collectAsState(initial = null)
            GymTrackerTheme {
                // Held back with the onboarding flag, so a runner in miles never sees a frame in km
                val unit = distanceUnit ?: return@GymTrackerTheme
                CompositionLocalProvider(LocalDistanceUnit provides unit) {
                    showOnboarding.value?.let { onboarding ->
                        NavGraph(
                            runToResume = runToResume.value,
                            showOnboarding = onboarding,
                            onFirstScreenReady = { firstScreenReady = true }
                        )
                    }
                }
            }
        }
    }

    /**
     * Hebrew is the default, but Android starts a fresh install in the system language.
     * Set from the activity, not the Application: a change made before the first activity
     * exists only half lands (right-to-left layout with English strings until a restart).
     * Returns true when the activity is about to be recreated with the new locale.
     */
    // A workout or run just ended, or the app is put away: upload what changed (only when signed in
    // and consented; otherwise the job does nothing).
    override fun onStop() {
        super.onStop()
        SyncWorker.syncSoon(this)
    }

    private fun applyDefaultLocale(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val localeManager = getSystemService(LocaleManager::class.java) ?: return false
        if (!localeManager.applicationLocales.isEmpty) return false
        localeManager.applicationLocales = LocaleList.forLanguageTags("iw")
        recreate()
        return true
    }

    private fun isUpdate(): Boolean {
        val info = packageManager.getPackageInfo(packageName, 0)
        return info.firstInstallTime != info.lastUpdateTime
    }
}
