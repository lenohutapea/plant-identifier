package com.lenohutapea.plantidentifier.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lenohutapea.plantidentifier.ui.screens.HistoryScreen
import com.lenohutapea.plantidentifier.ui.screens.HomeScreen
import com.lenohutapea.plantidentifier.ui.screens.ResultScreen

private object Routes {
    const val HOME = "home"
    const val RESULT = "result"
    const val HISTORY = "history"
}

@Composable
fun PlantIdentifierNavHost(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val navController = rememberNavController()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // A new result (from the API or from history) opens the result screen.
    LaunchedEffect(state) {
        if (state is AnalysisState.Success && navController.currentDestination?.route != Routes.RESULT) {
            navController.navigate(Routes.RESULT)
        }
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                state = state,
                onImageSelected = viewModel::analyze,
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onDismissError = viewModel::reset,
            )
        }
        composable(Routes.RESULT) {
            val current = state
            if (current is AnalysisState.Success) {
                ResultScreen(
                    diagnosis = current.diagnosis,
                    onBack = {
                        viewModel.reset()
                        navController.popBackStack()
                    },
                )
            } else {
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                entries = history,
                onOpen = viewModel::openHistory,
                onDelete = viewModel::delete,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
