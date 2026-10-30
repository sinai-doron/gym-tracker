package com.doron.gymtracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.doron.gymtracker.data.CardioRun
import com.doron.gymtracker.data.prefs.SettingsPrefs
import com.doron.gymtracker.domain.usecase.GetLastWeightUseCase
import com.doron.gymtracker.domain.usecase.LogSetUseCase
import com.doron.gymtracker.domain.usecase.ProgressionHintUseCase
import com.doron.gymtracker.domain.usecase.UpdateSetUseCase
import com.doron.gymtracker.ui.screens.cardio.CardioSessionScreen
import com.doron.gymtracker.ui.screens.cardio.CardioSessionViewModel
import com.doron.gymtracker.ui.screens.cardio.GpsLockScreen
import com.doron.gymtracker.ui.screens.cardio.GpsLockViewModel
import com.doron.gymtracker.ui.screens.history.SessionDetailScreen
import com.doron.gymtracker.ui.screens.history.SessionDetailViewModel
import com.doron.gymtracker.ui.screens.home.HomeScreen
import com.doron.gymtracker.ui.screens.plan.PlanEditorScreen
import com.doron.gymtracker.ui.screens.progress.ExerciseProgressScreen
import com.doron.gymtracker.ui.screens.session.SessionScreen
import com.doron.gymtracker.ui.screens.session.SessionViewModel
import com.doron.gymtracker.ui.screens.settings.SettingsScreen
import com.doron.gymtracker.ui.screens.routes.RouteDetailScreen
import com.doron.gymtracker.ui.screens.routes.RoutesScreen
import com.doron.gymtracker.ui.screens.workouts.WorkoutsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Composable
fun NavGraph(runToResume: CardioRun? = null) {
    val navController = rememberNavController()
    LaunchedEffect(runToResume) {
        runToResume?.let { run ->
            navController.navigate("cardio/${run.sessionId}/${run.type}") { launchSingleTop = true }
        }
    }
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onSessionStart = { sessionId, workoutId ->
                    navController.navigate("session/$sessionId/$workoutId")
                },
                onCardioStart = { type ->
                    navController.navigate("gps-lock/$type")
                },
                onChooseWorkout = { navController.navigate("workouts") },
                onExerciseProgress = { id -> navController.navigate("exercise-progress/$id") },
                onHistoryClick = { navController.navigate("history") },
                onRoutesClick = { navController.navigate("routes") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("workouts") {
            WorkoutsScreen(
                onBack = { navController.popBackStack() },
                onExerciseProgress = { id -> navController.navigate("exercise-progress/$id") },
                onEditPlan = { workoutId -> navController.navigate("plan/$workoutId") },
                onSessionStart = { sessionId, workoutId ->
                    // Finishing the session should land back on home, not on this screen
                    navController.navigate("session/$sessionId/$workoutId") {
                        popUpTo("workouts") { inclusive = true }
                    }
                }
            )
        }
        composable("plan/{workoutId}") {
            PlanEditorScreen(onBack = { navController.popBackStack() })
        }
        composable("exercise-progress/{exerciseId}") {
            ExerciseProgressScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
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
                updateSetUseCase = factoryHolder.updateSetUseCase,
                progressionHintUseCase = factoryHolder.progressionHintUseCase,
                getLastWeightUseCase = factoryHolder.getLastWeightUseCase,
                settingsPrefs = factoryHolder.settingsPrefs,
                onShowProgress = { id -> navController.navigate("exercise-progress/$id") },
                onFinished = { navController.popBackStack() }
            )
        }
        composable("gps-lock/{type}") { backStackEntry ->
            val type = backStackEntry.arguments?.getString("type") ?: return@composable
            val viewModel = hiltViewModel<GpsLockViewModel>()
            androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.startListening() }
            GpsLockScreen(
                type = type,
                viewModel = viewModel,
                onReady = { sessionId, readyType ->
                    navController.navigate("cardio/$sessionId/$readyType") {
                        popUpTo("gps-lock/$type") { inclusive = true }
                    }
                },
                onManageRoutes = { navController.navigate("routes") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("cardio/{sessionId}/{type}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
            val type = backStackEntry.arguments?.getString("type") ?: return@composable
            val viewModel = hiltViewModel<CardioSessionViewModel, CardioSessionViewModel.Factory>(
                creationCallback = { factory -> factory.create(sessionId, type) }
            )
            CardioSessionScreen(
                viewModel = viewModel,
                onFinished = { navController.popBackStack() }
            )
        }
        composable("routes") {
            RoutesScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onOpenRoute = { id -> navController.navigate("routes/$id") }
            )
        }
        composable("routes/{routeId}") {
            RouteDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
                onRunRoute = {
                    navController.navigate("gps-lock/RUNNING") { popUpTo("home") }
                }
            )
        }
        composable("history") {
            com.doron.gymtracker.ui.screens.history.HistoryScreen(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate("history/session/$sessionId")
                }
            )
        }
        composable("history/session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: return@composable
            val viewModel = hiltViewModel<SessionDetailViewModel>()
            androidx.compose.runtime.LaunchedEffect(sessionId) { viewModel.load(sessionId) }
            SessionDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@HiltViewModel
class SessionViewModelFactoryHolder @Inject constructor(
    val factory: SessionViewModel.Factory,
    val logSetUseCase: LogSetUseCase,
    val updateSetUseCase: UpdateSetUseCase,
    val progressionHintUseCase: ProgressionHintUseCase,
    val getLastWeightUseCase: GetLastWeightUseCase,
    val settingsPrefs: SettingsPrefs
) : ViewModel()
