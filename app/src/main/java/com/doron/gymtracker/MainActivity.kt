package com.doron.gymtracker

import android.os.Bundle
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
}
