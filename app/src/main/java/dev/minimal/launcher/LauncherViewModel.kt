package dev.minimal.launcher

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.minimal.launcher.data.AppIcons
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.LauncherSettings
import dev.minimal.launcher.data.Prefs
import dev.minimal.launcher.data.WallpaperMode
import java.io.File
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

enum class Screen { Home, Drawer, Settings, WidgetPicker }

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = Prefs(application)
    private val wallpaperFile = File(application.filesDir, "wallpaper.jpg")

    val widgetManager: AppWidgetManager = AppWidgetManager.getInstance(application)
    val widgetHost = AppWidgetHost(application, WIDGET_HOST_ID)

    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen

    private val _homePage = MutableStateFlow(0)
    val homePage: StateFlow<Int> = _homePage

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _pinned = MutableStateFlow(prefs.pinned)
    val pinned: StateFlow<List<String>> = _pinned

    private val _dock = MutableStateFlow(prefs.dock)
    val dock: StateFlow<List<String>> = _dock

    private val _hidden = MutableStateFlow(prefs.hidden)
    val hidden: StateFlow<Set<String>> = _hidden

    private val _settings = MutableStateFlow(prefs.loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings

    private val _widgetIds = MutableStateFlow(
        prefs.widgetIds.filter { widgetManager.getAppWidgetInfo(it) != null },
    )
    val widgetIds: StateFlow<List<Int>> = _widgetIds

    private val _wallpaperImage = MutableStateFlow<ImageBitmap?>(null)
    val wallpaperImage: StateFlow<ImageBitmap?> = _wallpaperImage

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
        loadWallpaperImage()
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(packageReceiver)
    }

    fun refreshApps() {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.Default) {
            AppIcons.clear()
            _apps.value = loadLaunchableApps(app)
        }
    }

    fun show(screen: Screen) {
        _screen.value = screen
    }

    fun goHome() = show(Screen.Home)

    fun setHomePage(page: Int) {
        _homePage.value = page
    }

    fun updateSettings(change: LauncherSettings.() -> LauncherSettings) {
        prefs.saveSettings(_settings.updateAndGet { it.change() })
    }

    fun togglePin(packageName: String) {
        val updated = _pinned.value.let { if (packageName in it) it - packageName else it + packageName }
        prefs.pinned = updated
        _pinned.value = updated
    }

    fun toggleDock(packageName: String) {
        val updated = _dock.value.let {
            if (packageName in it) it - packageName else (it + packageName).takeLast(DOCK_SIZE)
        }
        prefs.dock = updated
        _dock.value = updated
    }

    fun hide(packageName: String) {
        val updated = _hidden.value + packageName
        prefs.hidden = updated
        _hidden.value = updated
        if (packageName in _pinned.value) togglePin(packageName)
        if (packageName in _dock.value) toggleDock(packageName)
    }

    fun unhide(packageName: String) {
        val updated = _hidden.value - packageName
        prefs.hidden = updated
        _hidden.value = updated
    }

    fun addWidget(id: Int) {
        val updated = _widgetIds.value + id
        prefs.widgetIds = updated
        _widgetIds.value = updated
    }

    fun removeWidget(id: Int) {
        widgetHost.deleteAppWidgetId(id)
        val updated = _widgetIds.value - id
        prefs.widgetIds = updated
        _widgetIds.value = updated
    }

    fun setWallpaperFromUri(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = decodeForScreen(app, uri) ?: return@launch
            wallpaperFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            _wallpaperImage.value = bitmap.asImageBitmap()
            prefs.saveSettings(
                _settings.updateAndGet { it.copy(wallpaper = WallpaperMode.Image) },
            )
        }
    }

    private fun loadWallpaperImage() {
        viewModelScope.launch(Dispatchers.IO) {
            if (wallpaperFile.exists()) {
                _wallpaperImage.update { BitmapFactory.decodeFile(wallpaperFile.path)?.asImageBitmap() }
            }
        }
    }

    private fun decodeForScreen(context: Context, uri: Uri): Bitmap? {
        val metrics = context.resources.displayMetrics
        val target = max(metrics.widthPixels, metrics.heightPixels)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
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

    private companion object {
        const val WIDGET_HOST_ID = 1024
        const val DOCK_SIZE = 5
    }
}
