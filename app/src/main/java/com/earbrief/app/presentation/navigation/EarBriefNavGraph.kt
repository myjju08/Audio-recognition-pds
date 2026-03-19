package com.earbrief.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.earbrief.app.R
import com.earbrief.app.presentation.ui.main.MainScreen
import com.earbrief.app.presentation.ui.onboarding.OnboardingScreen
import com.earbrief.app.presentation.ui.triggerlog.TriggerLogScreen

private data class BottomNavItem(
    val labelRes: Int,
    val icon: ImageVector,
    val screen: Screen
)

private val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Default.Home, Screen.Home),
    BottomNavItem(R.string.nav_log, Icons.Default.List, Screen.TriggerLog),
    BottomNavItem(R.string.nav_knowledge, Icons.Default.Psychology, Screen.Knowledge),
    BottomNavItem(R.string.nav_profile, Icons.Default.Person, Screen.Profile),
    BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Screen.Settings)
)

@Composable
fun EarBriefNavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("earbrief_prefs", 0)
    val startDestination = if (prefs.getBoolean("onboarding_complete", false)) Screen.Home else Screen.Onboarding
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.hasRoute(Screen.Onboarding::class) != true

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hasRoute(item.screen::class) == true,
                            onClick = {
                                navController.navigate(item.screen) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding)) {
            composable<Screen.Onboarding> {
                OnboardingScreen(onComplete = {
                    navController.navigate(Screen.Home)
                })
            }
            composable<Screen.Home> {
                MainScreen(onOpenLog = { navController.navigate(Screen.TriggerLog) })
            }
            composable<Screen.TriggerLog> { TriggerLogScreen() }
            composable<Screen.Knowledge> { ComingSoonScreen(stringResource(R.string.coming_soon_knowledge)) }
            composable<Screen.Profile> { ComingSoonScreen(stringResource(R.string.coming_soon_profile)) }
            composable<Screen.Settings> { ComingSoonScreen(stringResource(R.string.coming_soon_settings)) }
        }
    }
}

@Composable
private fun ComingSoonScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
