package dev.minimal.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimal.launcher.LauncherViewModel
import dev.minimal.launcher.Screen
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.util.launchApp
import dev.minimal.launcher.util.openAppInfo
import dev.minimal.launcher.util.openDefaultLauncherSettings
import dev.minimal.launcher.util.openSystemSettings
import dev.minimal.launcher.util.uninstallApp

@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val allApps by viewModel.apps.collectAsStateWithLifecycle()
    val pinnedKeys by viewModel.pinned.collectAsStateWithLifecycle()
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val visibleApps = remember(allApps, hidden) { allApps.filter { it.packageName !in hidden } }
    val hiddenApps = remember(allApps, hidden) { allApps.filter { it.packageName in hidden } }
    val pinnedApps = remember(visibleApps, pinnedKeys) {
        val byPackage = visibleApps.associateBy { it.packageName }
        pinnedKeys.mapNotNull(byPackage::get)
    }

    val launch = { app: AppInfo ->
        context.launchApp(app)
        viewModel.goHome()
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Crossfade(targetState = screen, label = "screen") { current ->
            when (current) {
                Screen.Home -> {
                    // A launcher has nowhere to go back to.
                    BackHandler {}
                    HomeScreen(
                        pinned = pinnedApps,
                        onLaunch = launch,
                        onUnpin = { viewModel.togglePin(it.packageName) },
                        onOpenDrawer = { viewModel.show(Screen.Drawer) },
                        onOpenSettings = { viewModel.show(Screen.Settings) },
                    )
                }

                Screen.Drawer -> DrawerScreen(
                    apps = visibleApps,
                    pinned = pinnedKeys.toSet(),
                    onLaunch = launch,
                    onTogglePin = { viewModel.togglePin(it.packageName) },
                    onHide = { viewModel.hide(it.packageName) },
                    onAppInfo = context::openAppInfo,
                    onUninstall = context::uninstallApp,
                    onClose = viewModel::goHome,
                )

                Screen.Settings -> SettingsScreen(
                    themeMode = themeMode,
                    onThemeMode = viewModel::setThemeMode,
                    hiddenApps = hiddenApps,
                    onUnhide = { viewModel.unhide(it.packageName) },
                    onDefaultLauncher = context::openDefaultLauncherSettings,
                    onSystemSettings = context::openSystemSettings,
                    onClose = viewModel::goHome,
                )
            }
        }
    }
}
