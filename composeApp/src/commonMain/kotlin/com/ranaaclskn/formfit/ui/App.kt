package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.model.exerciseById

private enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    WORKOUT("workout", "Antrenman", Icons.Filled.FitnessCenter),
    LIBRARY("library", "Hareketler", Icons.Filled.VideoLibrary),
    HISTORY("history", "Geçmiş", Icons.Filled.History),
}

@Composable
fun App() {
    LaunchedEffect(Unit) { HistoryRepository.load() }
    FormFitTheme {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val showBar = Tab.entries.any { it.route == route }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { if (showBar) BottomBar(nav, route) },
        ) { padding ->
            NavHost(nav, startDestination = Tab.WORKOUT.route, modifier = Modifier.padding(padding)) {
                composable(Tab.WORKOUT.route) {
                    WorkoutHomeScreen(onStart = { nav.navigate("session-live/$it") })
                }
                composable(Tab.LIBRARY.route) {
                    LibraryScreen(onOpen = { nav.navigate("exercise/$it") })
                }
                composable(Tab.HISTORY.route) {
                    HistoryScreen(onOpen = { nav.navigate("session/$it") })
                }
                composable("exercise/{id}") { e ->
                    val ex = exerciseById(e.savedStateHandle.get<String>("id").orEmpty()) ?: return@composable
                    ExerciseDetailScreen(
                        exercise = ex,
                        onBack = { nav.popBackStack() },
                        onStartWorkout = { nav.navigate("session-live/${ex.id}") },
                    )
                }
                composable("session-live/{id}") { e ->
                    val ex = exerciseById(e.savedStateHandle.get<String>("id").orEmpty()) ?: return@composable
                    WorkoutScreen(
                        exercise = ex,
                        onClose = { nav.popBackStack() },
                        onShowHowTo = { nav.navigate("exercise/${ex.id}") },
                    )
                }
                composable("session/{id}") { e ->
                    SessionDetailScreen(
                        sessionId = e.savedStateHandle.get<String>("id").orEmpty(),
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = FfColors.Surface) {
        Tab.entries.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    indicatorColor = FfColors.Accent,
                ),
            )
        }
    }
}
