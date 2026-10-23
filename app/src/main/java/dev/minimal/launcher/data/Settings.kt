package dev.minimal.launcher.data

enum class ThemeMode { System, Light, Dark }

enum class WallpaperMode { None, System, Image }

enum class ClockFormat { System, H12, H24 }

enum class Accent(val lightArgb: Int, val darkArgb: Int) {
    Mono(0xFF141414.toInt(), 0xFFEDEDEA.toInt()),
    Blue(0xFF2563EB.toInt(), 0xFF7AA2FF.toInt()),
    Green(0xFF16A34A.toInt(), 0xFF5BD68A.toInt()),
    Amber(0xFFD97706.toInt(), 0xFFFFB454.toInt()),
    Rose(0xFFE11D48.toInt(), 0xFFFF7A95.toInt()),
    Violet(0xFF7C3AED.toInt(), 0xFFB794FF.toInt()),
}

data class LauncherSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val accent: Accent = Accent.Mono,
    val wallpaper: WallpaperMode = WallpaperMode.None,
    val wallpaperDim: Float = 0.35f,
    val clockFormat: ClockFormat = ClockFormat.System,
    val showDate: Boolean = true,
    val centerHome: Boolean = false,
    val drawerIcons: Boolean = true,
    val animations: Boolean = true,
    val haptics: Boolean = true,
)
