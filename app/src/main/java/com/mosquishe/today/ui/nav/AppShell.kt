package com.mosquishe.today.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mosquishe.today.di.appContainer
import com.mosquishe.today.domain.TaskView
import com.mosquishe.today.ui.detail.TaskDetailScreen
import com.mosquishe.today.ui.list.TaskListScreen
import com.mosquishe.today.ui.settings.SettingsScreen
import com.mudita.mmd.components.nav_bar.NavigationBarItemMMD
import com.mudita.mmd.components.nav_bar.NavigationBarMMD
import com.mudita.mmd.components.snackbar.SnackbarHostMMD
import com.mudita.mmd.components.snackbar.SnackbarHostStateMMD
import com.mudita.mmd.components.snackbar.SnackbarMMD
import com.mudita.mmd.components.snackbar.SnackbarResultMMD
import com.mudita.mmd.components.text.TextMMD
import kotlinx.coroutines.flow.MutableStateFlow

/** Root layout: a bottom nav bar over a no-animation NavHost. Bottom bar shows only on tab routes. */
@Composable
fun AppShell(deepLinkTaskId: MutableStateFlow<Long?> = remember { MutableStateFlow(null) }) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBar = currentRoute in Routes.TAB_ROUTES

    val container = appContainer()
    val snackbarState = remember { SnackbarHostStateMMD() }
    LaunchedEffect(Unit) {
        container.deletedTaskEvents.collect { snapshot ->
            val result = snackbarState.showSnackbar("To-do deleted", "Undo")
            if (result == SnackbarResultMMD.ActionPerformed) container.repository.restore(snapshot)
        }
    }

    // Open the to-do tapped in a reminder notification, then clear the request so it fires once.
    val pendingTaskId by deepLinkTaskId.collectAsState()
    LaunchedEffect(pendingTaskId) {
        pendingTaskId?.let { id ->
            nav.navigate(Routes.detail(id))
            deepLinkTaskId.value = null
        }
    }

    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(
                navController = nav,
                startDestination = Routes.TODAY,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                tab(Routes.TODAY, TaskView.TODAY, nav)
                tab(Routes.UPCOMING, TaskView.UPCOMING, nav)
                tab(Routes.ANYTIME, TaskView.ANYTIME, nav)
                tab(Routes.LOGBOOK, TaskView.LOGBOOK, nav)
                composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
                composable(
                    Routes.DETAIL,
                    arguments = listOf(
                        navArgument("taskId") { type = NavType.LongType },
                        navArgument("default") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    TaskDetailScreen(
                        taskId = entry.arguments?.getLong("taskId") ?: 0L,
                        defaultEpochDay = entry.arguments?.getLong("default") ?: -1L,
                        onBack = { nav.popBackStack() },
                    )
                }
            }
        }
        if (showBar) {
            NavigationBarMMD {
                TabItem(nav, currentRoute, Routes.TODAY, "Today", Icons.Filled.Today)
                TabItem(nav, currentRoute, Routes.UPCOMING, "Upcoming", Icons.Filled.CalendarMonth)
                TabItem(nav, currentRoute, Routes.ANYTIME, "Anytime", Icons.Filled.Inbox)
                TabItem(nav, currentRoute, Routes.LOGBOOK, "Logbook", Icons.Filled.CheckCircle)
            }
        }
        }

        SnackbarHostMMD(
            snackbarState,
            Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data -> SnackbarMMD(data) }
    }
}

private fun NavGraphBuilder.tab(route: String, view: TaskView, nav: NavHostController) {
    composable(route) {
        TaskListScreen(
            view = view,
            onOpenTask = { taskId, defaultEpochDay -> nav.navigate(Routes.detail(taskId, defaultEpochDay)) },
            onOpenSettings = { nav.navigate(Routes.SETTINGS) },
        )
    }
}

@Composable
private fun RowScope.TabItem(
    nav: NavHostController,
    currentRoute: String?,
    route: String,
    label: String,
    icon: ImageVector,
) {
    NavigationBarItemMMD(
        selected = currentRoute == route,
        onClick = {
            if (currentRoute != route) {
                nav.navigate(route) {
                    popUpTo(Routes.TODAY) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { TextMMD(label) },
    )
}
