package com.doron.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.doron.gymtracker.data.repository.WorkoutRepository
import com.doron.gymtracker.ui.navigation.NavGraph
import com.doron.gymtracker.ui.theme.GymTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workoutRepository: WorkoutRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch { workoutRepository.ensureSeeded() }
        enableEdgeToEdge()
        setContent {
            GymTrackerTheme {
                NavGraph()
            }
        }
    }
}
