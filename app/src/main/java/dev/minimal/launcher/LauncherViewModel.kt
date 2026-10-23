package dev.minimal.launcher

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.Prefs
import dev.minimal.launcher.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Screen { Home, Drawer, Settings }

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application)

    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _pinned = MutableStateFlow(prefs.pinned)
    val pinned: StateFlow<List<String>> = _pinned

    private val _hidden = MutableStateFlow(prefs.hidden)
    val hidden: StateFlow<Set<String>> = _hidden

    private val _themeMode = MutableStateFlow(prefs.themeMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshApps()
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            application,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        refreshApps()
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    fun refreshApps() {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.Default) {
            _apps.value = loadLaunchableApps(app)
        }
    }

    fun show(screen: Screen) {
        _screen.value = screen
    }

    fun goHome() = show(Screen.Home)

    fun setThemeMode(mode: ThemeMode) {
        prefs.themeMode = mode
        _themeMode.value = mode
    }

    fun togglePin(packageName: String) {
        val updated = _pinned.value.let { if (packageName in it) it - packageName else it + packageName }
        prefs.pinned = updated
        _pinned.value = updated
    }

    fun hide(packageName: String) {
        val updated = _hidden.value + packageName
        prefs.hidden = updated
        _hidden.value = updated
        if (packageName in _pinned.value) togglePin(packageName)
    }

    fun unhide(packageName: String) {
        val updated = _hidden.value - packageName
        prefs.hidden = updated
        _hidden.value = updated
    }

    private fun loadLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map {
                AppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName,
                    activityName = it.activityInfo.name,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
