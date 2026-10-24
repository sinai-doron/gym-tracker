package com.doron.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.ui.screens.home.HomeScreen
import com.doron.gymtracker.ui.screens.session.SessionScreen
import com.doron.gymtracker.ui.screens.session.SessionViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSessionStart = { sessionId, workoutId ->
                    navController.navigate("session/$sessionId/$workoutId")
                },
                onHistoryClick = { navController.navigate("history") }
            )
        }
        composable("session/{sessionId}/{workoutId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
            val workoutId = backStackEntry.arguments?.getString("workoutId") ?: return@composable
            val factoryHolder = hiltViewModel<SessionViewModelFactoryHolder>()
            val viewModel: SessionViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        factoryHolder.factory.create(sessionId, workoutId) as T
                }
            )
            SessionScreen(
                viewModel = viewModel,
                logSetUseCase = factoryHolder.logSetUseCase,
                onFinished = { navController.popBackStack() }
            )
        }
        composable("history") {
            com.doron.gymtracker.ui.screens.history.HistoryScreen()
        }
    }
}

@HiltViewModel
class SessionViewModelFactoryHolder @Inject constructor(
    val factory: SessionViewModel.Factory,
    val logSetUseCase: LogSetUseCase
) : ViewModel()
