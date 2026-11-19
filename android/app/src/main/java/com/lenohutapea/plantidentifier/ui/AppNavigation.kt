package com.lenohutapea.plantidentifier.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lenohutapea.plantidentifier.data.Diagnosis
import com.lenohutapea.plantidentifier.ui.screens.HistoryScreen
import com.lenohutapea.plantidentifier.ui.screens.HomeScreen
import com.lenohutapea.plantidentifier.ui.screens.ResultScreen
import com.lenohutapea.plantidentifier.ui.screens.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

/** Pops only when [route] is on top, so a second call during an exit animation cannot empty the back stack. */
private fun NavHostController.popIfOn(route: String) {
    if (currentDestination?.route == route) popBackStack()
}

@Composable
fun PlantIdentifierNavHost(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val plants by viewModel.plants.collectAsStateWithLifecycle()

    // The result screen keeps showing the last diagnosis while it animates out, even after the state is reset.
    var lastDiagnosis by remember { mutableStateOf<Diagnosis?>(null) }

    LaunchedEffect(state) {
        val current = state
        if (current is AnalysisState.Success) {
            lastDiagnosis = current.diagnosis
            if (navController.currentDestination?.route != Routes.RESULT) navController.navigate(Routes.RESULT)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            LaunchedEffect(Unit) { viewModel.refreshPlants() }
            HomeScreen(
                state = state,
                recent = history.take(6),
                plants = plants,
                onImageSelected = viewModel::analyze,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRecent = viewModel::openHistory,
                onDismissError = viewModel::reset,
            )
        }
        composable(Routes.RESULT) {
            lastDiagnosis?.let { diagnosis ->
                ResultScreen(
                    diagnosis = diagnosis,
                    onBack = {
                        navController.popIfOn(Routes.RESULT)
                        viewModel.reset()
                    },
                )
            }
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                entries = history,
                onOpen = viewModel::openHistory,
                onDelete = viewModel::delete,
                onTakePhoto = { navController.popIfOn(Routes.HISTORY) },
                onBack = { navController.popIfOn(Routes.HISTORY) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel(factory = SettingsViewModel.Factory),
                onBack = {
                    navController.popIfOn(Routes.SETTINGS)
                    viewModel.refreshPlants()
                },
            )
        }
    }
}
