package dev.minimal.launcher.ui

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.minimal.launcher.data.Accent
import dev.minimal.launcher.data.LauncherSettings
import dev.minimal.launcher.data.ThemeMode
import dev.minimal.launcher.data.WallpaperMode

val LocalSettings = staticCompositionLocalOf { LauncherSettings() }
val LocalAccent = compositionLocalOf { Color.Unspecified }
val LocalDark = staticCompositionLocalOf { false }

fun Accent.color(dark: Boolean): Color = Color(if (dark) darkArgb else lightArgb)

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
fun LauncherTheme(settings: LauncherSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val target = if (dark) DarkColors else LightColors

    // Colors glide between light and dark instead of snapping.
    val duration = if (settings.animations) 350 else 0
    val background by animateColorAsState(target.background, tween(duration), label = "background")
    val foreground by animateColorAsState(target.onBackground, tween(duration), label = "foreground")
    val container by animateColorAsState(target.surfaceContainer, tween(duration), label = "container")
    val dim by animateColorAsState(target.onSurfaceVariant, tween(duration), label = "dim")
    val scheme = target.copy(
        background = background,
        surface = background,
        onBackground = foreground,
        onSurface = foreground,
        surfaceContainer = container,
        onSurfaceVariant = dim,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // The theme sets windowShowWallpaper; a transparent window lets it through.
            val windowColor = if (settings.wallpaper == WallpaperMode.System) {
                android.graphics.Color.TRANSPARENT
            } else {
                target.background.toArgb()
            }
            window.setBackgroundDrawable(ColorDrawable(windowColor))
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(
        LocalSettings provides settings,
        LocalAccent provides settings.accent.color(dark),
        LocalDark provides dark,
    ) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
