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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.earbrief.app.presentation.ui.main.MainScreen

private data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen
)

private val bottomNavItems = listOf(
    BottomNavItem("홈", Icons.Default.Home, Screen.Home),
    BottomNavItem("로그", Icons.Default.List, Screen.TriggerLog),
    BottomNavItem("지식", Icons.Default.Psychology, Screen.Knowledge),
    BottomNavItem("프로필", Icons.Default.Person, Screen.Profile),
    BottomNavItem("설정", Icons.Default.Settings, Screen.Settings),
)

@Composable
fun EarBriefNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hasRoute(item.screen::class) == true,
                        onClick = {
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screen.Home> {
                MainScreen()
            }
            composable<Screen.TriggerLog> {
                PlaceholderScreen("트리거 로그")
            }
            composable<Screen.Knowledge> {
                PlaceholderScreen("지식 관리")
            }
            composable<Screen.Profile> {
                PlaceholderScreen("프로필")
            }
            composable<Screen.Settings> {
                PlaceholderScreen("설정")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
    }
}
