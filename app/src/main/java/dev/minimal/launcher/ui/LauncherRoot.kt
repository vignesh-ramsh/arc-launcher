package dev.minimal.launcher.ui

import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimal.launcher.LauncherViewModel
import dev.minimal.launcher.Screen
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.LauncherSettings
import dev.minimal.launcher.data.WallpaperMode
import dev.minimal.launcher.util.expandNotificationShade
import dev.minimal.launcher.util.launchApp
import dev.minimal.launcher.util.openAppInfo
import dev.minimal.launcher.util.openDefaultLauncherSettings
import dev.minimal.launcher.util.openSystemSettings
import dev.minimal.launcher.util.openSystemWallpaperPicker
import dev.minimal.launcher.util.uninstallApp

@Composable
fun LauncherRoot(
    viewModel: LauncherViewModel,
    onPickWallpaper: () -> Unit,
    onAddWidget: (AppWidgetProviderInfo) -> Unit,
) {
    val context = LocalContext.current
    val settings = LocalSettings.current
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val pinnedKeys by viewModel.pinned.collectAsStateWithLifecycle()
    val dockKeys by viewModel.dock.collectAsStateWithLifecycle()
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    val widgetIds by viewModel.widgetIds.collectAsStateWithLifecycle()
    val wallpaperImage by viewModel.wallpaperImage.collectAsStateWithLifecycle()

    val visibleApps = remember(allApps, hidden) { allApps.filter { it.packageName !in hidden } }
    val hiddenApps = remember(allApps, hidden) { allApps.filter { it.packageName in hidden } }
    val byPackage = remember(visibleApps) { visibleApps.associateBy { it.packageName } }
    val pinnedApps = remember(byPackage, pinnedKeys) { pinnedKeys.mapNotNull(byPackage::get) }
    val dockApps = remember(byPackage, dockKeys) { dockKeys.mapNotNull(byPackage::get) }

    val launch = { app: AppInfo ->
        context.launchApp(app)
        viewModel.goHome()
    }

    // Keeps text readable on top of a wallpaper for every screen except home.
    val readableScrim = if (settings.wallpaper == WallpaperMode.None) {
        Modifier
    } else {
        Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
    }

    Box(Modifier.fillMaxSize()) {
        WallpaperLayer(settings, wallpaperImage)

        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                if (!settings.animations) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else when {
                    targetState == Screen.Drawer ->
                        (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it / 4 } +
                            fadeIn(tween(240))) togetherWith
                            (fadeOut(tween(160)) + scaleOut(targetScale = 0.96f, animationSpec = tween(240)))

                    initialState == Screen.Drawer ->
                        (fadeIn(tween(240)) + scaleIn(initialScale = 0.96f, animationSpec = tween(240))) togetherWith
                            (slideOutVertically(tween(260)) { it / 4 } + fadeOut(tween(200)))

                    targetState == Screen.Home ->
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 5 } +
                            fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(260)) { it / 5 } + fadeOut(tween(200)))

                    else ->
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 5 } +
                            fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(260)) { -it / 5 } + fadeOut(tween(200)))
                }
            },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Home -> {
                    // A launcher has nowhere to go back to.
                    BackHandler {}
                    HomeScreen(
                        pinned = pinnedApps,
                        dock = dockApps,
                        widgetIds = widgetIds,
                        widgetHost = viewModel.widgetHost,
                        widgetManager = viewModel.widgetManager,
                        initialPage = homePage,
                        onPageChanged = viewModel::setHomePage,
                        onLaunch = launch,
                        onUnpin = { viewModel.togglePin(it.packageName) },
                        onRemoveFromDock = { viewModel.toggleDock(it.packageName) },
                        onOpenDrawer = { viewModel.show(Screen.Drawer) },
                        onOpenSettings = { viewModel.show(Screen.Settings) },
                        onPullDown = { context.expandNotificationShade() },
                        onAddWidget = { viewModel.show(Screen.WidgetPicker) },
                        onRemoveWidget = viewModel::removeWidget,
                    )
                }

                Screen.Drawer -> Box(Modifier.fillMaxSize().then(readableScrim)) {
                    DrawerScreen(
                        apps = visibleApps,
                        pinned = pinnedKeys.toSet(),
                        docked = dockKeys.toSet(),
                        onLaunch = launch,
                        onTogglePin = { viewModel.togglePin(it.packageName) },
                        onToggleDock = { viewModel.toggleDock(it.packageName) },
                        onHide = { viewModel.hide(it.packageName) },
                        onAppInfo = context::openAppInfo,
                        onUninstall = context::uninstallApp,
                        onClose = viewModel::goHome,
                    )
                }

                Screen.Settings -> Box(Modifier.fillMaxSize().then(readableScrim)) {
                    SettingsScreen(
                        settings = settings,
                        onUpdate = viewModel::updateSettings,
                        hasWallpaperImage = wallpaperImage != null,
                        onPickWallpaper = onPickWallpaper,
                        onSystemWallpaper = context::openSystemWallpaperPicker,
                        hiddenApps = hiddenApps,
                        onUnhide = { viewModel.unhide(it.packageName) },
                        onDefaultLauncher = context::openDefaultLauncherSettings,
                        onSystemSettings = context::openSystemSettings,
                        onClose = viewModel::goHome,
                    )
                }

                Screen.WidgetPicker -> Box(Modifier.fillMaxSize().then(readableScrim)) {
                    WidgetPickerScreen(
                        manager = viewModel.widgetManager,
                        onPick = onAddWidget,
                        onClose = viewModel::goHome,
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperLayer(settings: LauncherSettings, image: ImageBitmap?) {
    val background = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize()) {
        when {
            settings.wallpaper == WallpaperMode.System -> Unit // the window itself is transparent
            settings.wallpaper == WallpaperMode.Image && image != null -> Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            else -> Box(Modifier.fillMaxSize().background(background))
        }
        if (settings.wallpaper != WallpaperMode.None) {
            Box(Modifier.fillMaxSize().background(background.copy(alpha = settings.wallpaperDim)))
        }
    }
}
