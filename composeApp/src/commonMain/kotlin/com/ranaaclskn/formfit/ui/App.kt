package com.ranaaclskn.formfit.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ranaaclskn.formfit.data.HistoryRepository
import com.ranaaclskn.formfit.data.PlanRepository
import com.ranaaclskn.formfit.data.runProgress
import com.ranaaclskn.formfit.model.exerciseById

private enum class Tab(val route: String, val title: String, val icon: ImageVector) {
    WORKOUT("workout", "Antrenman", Icons.Filled.FitnessCenter),
    PLANS("plans", "Programlar", Icons.Filled.CalendarMonth),
    LIBRARY("library", "Hareketler", Icons.Filled.VideoLibrary),
    HISTORY("history", "Geçmiş", Icons.Filled.History),
}

@Composable
fun App() {
    LaunchedEffect(Unit) {
        HistoryRepository.load()
        PlanRepository.load()
    }
    FormFitTheme {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        val showBar = Tab.entries.any { it.route == route }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            // Her ekran sistem çubuklarını kendisi ayarlar (antrenman ekranı kamerayı tam ekran gösterir)
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { if (showBar) BottomBar(nav, route) },
        ) { padding ->
            NavHost(nav, startDestination = Tab.WORKOUT.route, modifier = Modifier.padding(padding)) {
                composable(Tab.WORKOUT.route) {
                    WorkoutHomeScreen(onStart = { nav.navigate("session-live/$it") })
                }
                composable(Tab.PLANS.route) {
                    PlansScreen(
                        onOpen = { nav.navigate("plan/$it") },
                        onCreate = { nav.navigate("plan-edit/new") },
                        onRecommend = { nav.navigate("recommend") },
                    )
                }
                composable("recommend") {
                    RecommendScreen(
                        onBack = { nav.popBackStack() },
                        onSaved = { nav.popBackStack() },
                    )
                }
                composable("plan/{id}") { e ->
                    val id = e.savedStateHandle.get<String>("id").orEmpty()
                    PlanDetailScreen(
                        planId = id,
                        onBack = { nav.popBackStack() },
                        onEdit = { nav.navigate("plan-edit/$id") },
                        onStart = { runId -> nav.navigate("plan-run/$id/$runId") },
                        onOpenRun = { runId -> nav.navigate("plan-run/$id/$runId") },
                    )
                }
                composable("plan-edit/{id}") { e ->
                    val id = e.savedStateHandle.get<String>("id").orEmpty()
                    PlanEditScreen(
                        planId = id.takeIf { it != "new" },
                        onDone = { nav.popBackStack() },
                        onCancel = { nav.popBackStack() },
                    )
                }
                composable("plan-run/{planId}/{runId}") { e ->
                    val planId = e.savedStateHandle.get<String>("planId").orEmpty()
                    val runId = e.savedStateHandle.get<String>("runId").orEmpty()
                    PlanRunScreen(
                        planId = planId,
                        runId = runId,
                        onBack = { nav.popBackStack() },
                        onStartSet = { index -> nav.navigate("plan-set/$planId/$runId/$index") },
                    )
                }
                composable("plan-set/{planId}/{runId}/{index}") { e ->
                    val planId = e.savedStateHandle.get<String>("planId").orEmpty()
                    val runId = e.savedStateHandle.get<String>("runId").orEmpty()
                    val index = e.savedStateHandle.get<String>("index")?.toIntOrNull() ?: 0
                    // Set numarası ekran açılırken bir kez hesaplanır
                    val context = remember(planId, runId, index) {
                        val plan = PlanRepository.find(planId) ?: return@remember null
                        val item = plan.items.getOrNull(index) ?: return@remember null
                        val done = runProgress(plan, runId, HistoryRepository.sessions.value).items[index].doneSets
                        PlanContext(plan.id, plan.name, runId, index, done + 1, item.sets, item.reps)
                    }
                    val ex = context?.let { exerciseById(PlanRepository.find(planId)!!.items[index].exerciseId) }
                    if (context == null || ex == null) return@composable
                    WorkoutScreen(
                        exercise = ex,
                        onClose = { nav.popBackStack() },
                        onShowHowTo = { nav.navigate("exercise/${ex.id}") },
                        plan = context,
                    )
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
    Column {
        HorizontalDivider(color = FfColors.Border)
        NavigationBar(containerColor = FfColors.Background, tonalElevation = 0.dp) {
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
                    label = { Text(tab.title.trUpper(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp), maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FfColors.OnAccent,
                        selectedTextColor = FfColors.Text,
                        indicatorColor = FfColors.Accent,
                        unselectedIconColor = FfColors.TextMuted,
                        unselectedTextColor = FfColors.TextMuted,
                    ),
                )
            }
        }
    }
}
