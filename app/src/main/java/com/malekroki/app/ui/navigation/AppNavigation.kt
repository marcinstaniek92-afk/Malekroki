@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.malekroki.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.malekroki.app.ui.calendar.CalendarScreen
import com.malekroki.app.ui.habits.HabitEditScreen
import com.malekroki.app.ui.habits.HabitsScreen
import com.malekroki.app.ui.journal.JournalScreen
import com.malekroki.app.ui.journal.SessionEditScreen
import com.malekroki.app.ui.leave.LeaveEditScreen
import com.malekroki.app.ui.leave.LeaveListScreen
import com.malekroki.app.ui.settings.SettingsScreen
import com.malekroki.app.ui.today.TodayScreen

private object Routes {
    const val TODAY = "today"
    const val HABITS = "habits"
    const val CALENDAR = "calendar"
    const val JOURNAL = "journal"
    const val SETTINGS = "settings"
    const val HABIT_EDIT = "habit_edit?id={id}"
    const val SESSION_EDIT = "session_edit?id={id}&habitId={habitId}"
    const val LEAVES = "leaves"
    const val LEAVE_EDIT = "leave_edit?id={id}"

    fun habitEdit(id: String?) = "habit_edit?id=${id.orEmpty()}"
    fun sessionEdit(id: String? = null, habitId: String? = null) = "session_edit?id=${id.orEmpty()}&habitId=${habitId.orEmpty()}"
    fun leaveEdit(id: String?) = "leave_edit?id=${id.orEmpty()}"
}

private data class TopLevel(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.TODAY, "Dzisiaj", Icons.Outlined.WbSunny, Icons.Filled.WbSunny),
    TopLevel(Routes.HABITS, "Nawyki", Icons.Outlined.Checklist, Icons.Filled.Checklist),
    TopLevel(Routes.CALENDAR, "Kalendarz", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    TopLevel(Routes.JOURNAL, "Dziennik", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    TopLevel(Routes.SETTINGS, "Ustawienia", Icons.Outlined.Settings, Icons.Filled.Settings),
)

private fun optionalString(name: String) = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val showBottomBar = topLevel.any { it.route == currentRoute }

    Scaffold(
        // Top insets are handled by each screen's app bar; horizontal cutouts matter in landscape.
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevel.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.navigateTopLevel(item.route) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                            label = { Text(item.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.TODAY,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
        ) {
            composable(Routes.TODAY) {
                TodayScreen(
                    onStartSession = { habitId -> nav.navigate(Routes.sessionEdit(habitId = habitId)) },
                    onOpenSession = { id -> nav.navigate(Routes.sessionEdit(id = id)) },
                    onOpenJournal = { nav.navigateTopLevel(Routes.JOURNAL) },
                    onOpenLeaves = { nav.navigate(Routes.LEAVES) },
                    onAddHabit = { nav.navigate(Routes.habitEdit(null)) },
                )
            }
            composable(Routes.HABITS) {
                HabitsScreen(onEdit = { id -> nav.navigate(Routes.habitEdit(id)) })
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(onOpenLeaves = { nav.navigate(Routes.LEAVES) })
            }
            composable(Routes.JOURNAL) {
                JournalScreen(
                    onNewSession = { nav.navigate(Routes.sessionEdit()) },
                    onOpenSession = { id -> nav.navigate(Routes.sessionEdit(id = id)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onOpenLeaves = { nav.navigate(Routes.LEAVES) })
            }
            composable(Routes.HABIT_EDIT, arguments = listOf(optionalString("id"))) {
                HabitEditScreen(onDone = dropUnlessResumed { nav.popBackStack() })
            }
            composable(Routes.SESSION_EDIT, arguments = listOf(optionalString("id"), optionalString("habitId"))) {
                SessionEditScreen(onDone = dropUnlessResumed { nav.popBackStack() })
            }
            composable(Routes.LEAVES) {
                LeaveListScreen(onBack = dropUnlessResumed { nav.popBackStack() }, onEdit = { id -> nav.navigate(Routes.leaveEdit(id)) })
            }
            composable(Routes.LEAVE_EDIT, arguments = listOf(optionalString("id"))) {
                LeaveEditScreen(onDone = dropUnlessResumed { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
