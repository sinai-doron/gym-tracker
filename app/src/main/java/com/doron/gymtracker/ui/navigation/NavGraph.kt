package com.doron.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doron.gymtracker.ui.screens.home.HomeScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSessionStart = { sessionId -> navController.navigate("session/$sessionId") },
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("session/{sessionId}") {
            androidx.compose.material3.Text("Session — coming soon")
        }
        composable("history") {
            androidx.compose.material3.Text("History — coming soon")
        }
    }
}
