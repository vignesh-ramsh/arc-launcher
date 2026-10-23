package dev.minimal.launcher.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.minimal.launcher.data.ThemeMode

private val LightColors = lightColorScheme(
    background = Color(0xFFF6F5F2),
    onBackground = Color(0xFF141414),
    surface = Color(0xFFF6F5F2),
    onSurface = Color(0xFF141414),
    surfaceContainer = Color(0xFFEAE9E5),
    onSurfaceVariant = Color(0xFF8C8C88),
)

private val DarkColors = darkColorScheme(
    background = Color(0xFF0A0A0B),
    onBackground = Color(0xFFEDEDEA),
    surface = Color(0xFF0A0A0B),
    onSurface = Color(0xFFEDEDEA),
    surfaceContainer = Color(0xFF1A1A1C),
    onSurfaceVariant = Color(0xFF7C7C80),
)

@Composable
fun LauncherTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
