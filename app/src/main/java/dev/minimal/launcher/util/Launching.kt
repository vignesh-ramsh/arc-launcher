package dev.minimal.launcher.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dev.minimal.launcher.data.AppInfo

private fun Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // The app was removed or disabled between listing and launching.
    } catch (_: SecurityException) {
        // The target activity is no longer exported to us.
    }
}

fun Context.launchApp(app: AppInfo) {
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LAUNCHER)
        .setComponent(ComponentName(app.packageName, app.activityName))
    startSafely(intent)
}

fun Context.openAppInfo(app: AppInfo) {
    startSafely(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")),
    )
}

fun Context.uninstallApp(app: AppInfo) {
    startSafely(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
}

fun Context.openDefaultLauncherSettings() = startSafely(Intent(Settings.ACTION_HOME_SETTINGS))

fun Context.openSystemSettings() = startSafely(Intent(Settings.ACTION_SETTINGS))
