package dev.minimal.launcher.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ActivityNotFoundException
import android.content.Intent

/**
 * Walks one widget through allocate -> bind (asks the user if needed) -> configure (if the
 * provider has a config screen) and reports the id once it is ready to display.
 */
class WidgetController(
    private val activity: Activity,
    private val host: AppWidgetHost,
    private val manager: AppWidgetManager,
    private val onAdded: (Int) -> Unit,
) {
    private var pendingId = AppWidgetManager.INVALID_APPWIDGET_ID

    fun start(provider: AppWidgetProviderInfo) {
        cancel()
        val id = host.allocateAppWidgetId()
        pendingId = id
        if (manager.bindAppWidgetIdIfAllowed(id, provider.provider)) {
            configureOrFinish()
            return
        }
        val bind = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
        try {
            @Suppress("DEPRECATION")
            activity.startActivityForResult(bind, REQUEST_BIND)
        } catch (_: ActivityNotFoundException) {
            cancel()
        }
    }

    /** Returns true when the result belonged to a widget request. */
    fun handleResult(requestCode: Int, resultCode: Int): Boolean {
        if (requestCode != REQUEST_BIND && requestCode != REQUEST_CONFIGURE) return false
        when {
            resultCode != Activity.RESULT_OK -> cancel()
            requestCode == REQUEST_BIND -> configureOrFinish()
            else -> finish()
        }
        return true
    }

    private fun configureOrFinish() {
        val info = manager.getAppWidgetInfo(pendingId)
        if (info?.configure == null) {
            finish()
            return
        }
        try {
            host.startAppWidgetConfigureActivityForResult(
                activity,
                pendingId,
                0,
                REQUEST_CONFIGURE,
                null,
            )
        } catch (_: ActivityNotFoundException) {
            cancel()
        } catch (_: SecurityException) {
            cancel()
        }
    }

    private fun finish() {
        val id = pendingId
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) onAdded(id)
    }

    private fun cancel() {
        if (pendingId != AppWidgetManager.INVALID_APPWIDGET_ID) host.deleteAppWidgetId(pendingId)
        pendingId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private companion object {
        const val REQUEST_BIND = 4101
        const val REQUEST_CONFIGURE = 4102
    }
}
