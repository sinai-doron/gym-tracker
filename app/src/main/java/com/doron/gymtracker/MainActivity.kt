package com.doron.gymtracker

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import com.doron.gymtracker.sync.SyncWorker
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.CardioRun
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.ui.navigation.NavGraph
import com.doron.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var cardioRecorder: CardioRecorder
    @Inject lateinit var settingsPrefs: SettingsPrefs

    // A run still going (or cut off by the system) to reopen, found at launch
    private val runToResume = mutableStateOf<CardioRun?>(null)

    // Null until read, so neither home nor the wizard flashes up first
    private val showOnboarding = mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (applyDefaultLocale()) return  // recreated in Hebrew straight away
        enableEdgeToEdge()
        lifecycleScope.launch { workoutRepository.ensureSeeded() }
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
            GymTrackerTheme {
                showOnboarding.value?.let { onboarding ->
                    NavGraph(runToResume = runToResume.value, showOnboarding = onboarding)
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
