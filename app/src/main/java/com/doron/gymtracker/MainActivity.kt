package com.doron.gymtracker

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.doron.gymtracker.data.CardioRecorder
import com.doron.gymtracker.data.CardioRun
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.ui.navigation.NavGraph
import com.doron.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workoutRepository: WorkoutRepository
    @Inject lateinit var cardioRecorder: CardioRecorder

    // A run still going (or cut off by the system) to reopen, found at launch
    private val runToResume = mutableStateOf<CardioRun?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (applyDefaultLocale()) return  // recreated in Hebrew straight away
        enableEdgeToEdge()
        lifecycleScope.launch { workoutRepository.ensureSeeded() }
        if (savedInstanceState == null) {
            lifecycleScope.launch { runToResume.value = cardioRecorder.recoverOpenRun() }
        }
        setContent {
            GymTrackerTheme {
                NavGraph(runToResume = runToResume.value)
            }
        }
    }

    /**
     * Hebrew is the default, but Android starts a fresh install in the system language.
     * Set from the activity, not the Application: a change made before the first activity
     * exists only half lands (right-to-left layout with English strings until a restart).
     * Returns true when the activity is about to be recreated with the new locale.
     */
    private fun applyDefaultLocale(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        val localeManager = getSystemService(LocaleManager::class.java) ?: return false
        if (!localeManager.applicationLocales.isEmpty) return false
        localeManager.applicationLocales = LocaleList.forLanguageTags("iw")
        recreate()
        return true
    }
}
