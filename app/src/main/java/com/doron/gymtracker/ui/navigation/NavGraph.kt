package com.doron.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.Text

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { Text("Home — coming soon") }
        composable("session/{sessionId}") { Text("Session — coming soon") }
        composable("history") { Text("History — coming soon") }
    }
}
