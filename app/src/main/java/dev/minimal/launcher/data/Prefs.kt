package dev.minimal.launcher.data

import android.content.Context

enum class ThemeMode { System, Light, Dark }

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == sp.getString(KEY_THEME, null) }
            ?: ThemeMode.System
        set(value) = sp.edit().putString(KEY_THEME, value.name).apply()

    var pinned: List<String>
        get() = sp.getString(KEY_PINNED, "").orEmpty().split(SEP).filter { it.isNotEmpty() }
        set(value) = sp.edit().putString(KEY_PINNED, value.joinToString(SEP)).apply()

    var hidden: Set<String>
        get() = sp.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        set(value) = sp.edit().putStringSet(KEY_HIDDEN, value).apply()

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_PINNED = "pinned"
        const val KEY_HIDDEN = "hidden"
        const val SEP = "\n"
    }
}
