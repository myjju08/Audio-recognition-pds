package com.earbrief.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = WhisperAmber,
    onPrimary = MidnightBackground,
    primaryContainer = WhisperAmberContainer,
    onPrimaryContainer = WhisperAmber,
    secondary = WhisperPeriwinkle,
    onSecondary = MidnightBackground,
    secondaryContainer = WhisperPeriwinkleContainer,
    onSecondaryContainer = MidnightOnBackground,
    tertiary = WhisperMint,
    onTertiary = MidnightBackground,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceRaised,
    onSurfaceVariant = MidnightOnSurfaceMuted,
    outline = MidnightOutline,
    error = WhisperCoral,
    onError = MidnightBackground
)

private val LightColorScheme = lightColorScheme(
    primary = DawnPrimary,
    onPrimary = DawnBackground,
    primaryContainer = DawnPrimaryContainer,
    onPrimaryContainer = DawnPrimary,
    secondary = DawnSecondary,
    onSecondary = DawnBackground,
    tertiary = DawnTertiary,
    onTertiary = DawnBackground,
    background = DawnBackground,
    onBackground = DawnOnBackground,
    surface = DawnSurface,
    onSurface = DawnOnSurface,
    surfaceVariant = DawnSurfaceVariant,
    onSurfaceVariant = DawnOnSurfaceMuted,
    outline = DawnOutline,
    error = DawnError,
    onError = DawnBackground
)

@Composable
fun EarBriefTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme || isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EarBriefTypography,
        shapes = EarBriefShapes,
        content = content
    )
}
