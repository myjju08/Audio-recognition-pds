package com.earbrief.app.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable data object Home : Screen
    @Serializable data object TriggerLog : Screen
    @Serializable data object Knowledge : Screen
    @Serializable data object Profile : Screen
    @Serializable data object Settings : Screen
    @Serializable data object Onboarding : Screen
}
