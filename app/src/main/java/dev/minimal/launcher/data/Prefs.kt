package dev.minimal.launcher.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var pinned: List<String>
        get() = readList(KEY_PINNED)
        set(value) = writeList(KEY_PINNED, value)

    var dock: List<String>
        get() = readList(KEY_DOCK)
        set(value) = writeList(KEY_DOCK, value)

    var hidden: Set<String>
        get() = sp.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        set(value) = sp.edit().putStringSet(KEY_HIDDEN, value).apply()

    var widgetIds: List<Int>
        get() = readList(KEY_WIDGETS).mapNotNull { it.toIntOrNull() }
        set(value) = writeList(KEY_WIDGETS, value.map { it.toString() })

    fun loadSettings(): LauncherSettings {
        val d = LauncherSettings()
        return LauncherSettings(
            themeMode = readEnum(KEY_THEME, d.themeMode),
            accent = readEnum(KEY_ACCENT, d.accent),
            wallpaper = readEnum(KEY_WALLPAPER, d.wallpaper),
            wallpaperDim = sp.getFloat(KEY_WALLPAPER_DIM, d.wallpaperDim),
            clockFormat = readEnum(KEY_CLOCK, d.clockFormat),
            showDate = sp.getBoolean(KEY_SHOW_DATE, d.showDate),
            centerHome = sp.getBoolean(KEY_CENTER, d.centerHome),
            drawerIcons = sp.getBoolean(KEY_DRAWER_ICONS, d.drawerIcons),
            animations = sp.getBoolean(KEY_ANIMATIONS, d.animations),
            haptics = sp.getBoolean(KEY_HAPTICS, d.haptics),
        )
    }

    fun saveSettings(s: LauncherSettings) {
        sp.edit()
            .putString(KEY_THEME, s.themeMode.name)
            .putString(KEY_ACCENT, s.accent.name)
            .putString(KEY_WALLPAPER, s.wallpaper.name)
            .putFloat(KEY_WALLPAPER_DIM, s.wallpaperDim)
            .putString(KEY_CLOCK, s.clockFormat.name)
            .putBoolean(KEY_SHOW_DATE, s.showDate)
            .putBoolean(KEY_CENTER, s.centerHome)
            .putBoolean(KEY_DRAWER_ICONS, s.drawerIcons)
            .putBoolean(KEY_ANIMATIONS, s.animations)
            .putBoolean(KEY_HAPTICS, s.haptics)
            .apply()
    }

    private fun readList(key: String): List<String> =
        sp.getString(key, "").orEmpty().split(SEP).filter { it.isNotEmpty() }

    private fun writeList(key: String, value: List<String>) {
        sp.edit().putString(key, value.joinToString(SEP)).apply()
    }

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E {
        val name = sp.getString(key, null)
        return enumValues<E>().firstOrNull { it.name == name } ?: default
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_ACCENT = "accent"
        const val KEY_WALLPAPER = "wallpaper"
        const val KEY_WALLPAPER_DIM = "wallpaper_dim"
        const val KEY_CLOCK = "clock_format"
        const val KEY_SHOW_DATE = "show_date"
        const val KEY_CENTER = "center_home"
        const val KEY_DRAWER_ICONS = "drawer_icons"
        const val KEY_ANIMATIONS = "animations"
        const val KEY_HAPTICS = "haptics"
        const val KEY_PINNED = "pinned"
        const val KEY_DOCK = "dock"
        const val KEY_HIDDEN = "hidden"
        const val KEY_WIDGETS = "widgets"
        const val SEP = "\n"
    }
}
