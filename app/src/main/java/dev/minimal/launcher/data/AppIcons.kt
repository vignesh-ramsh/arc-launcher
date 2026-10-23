package dev.minimal.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** In-memory cache of launcher icons, keyed by package and pixel size. */
object AppIcons {
    private val cache = LruCache<String, ImageBitmap>(300)

    private fun key(app: AppInfo, px: Int) = "${app.packageName}/$px"

    fun peek(app: AppInfo, px: Int): ImageBitmap? = cache.get(key(app, px))

    fun load(context: Context, app: AppInfo, px: Int): ImageBitmap? {
        peek(app, px)?.let { return it }
        val drawable = try {
            context.packageManager.getActivityIcon(ComponentName(app.packageName, app.activityName))
        } catch (_: PackageManager.NameNotFoundException) {
            return null
        }
        val bitmap = drawable.toBitmap(px, px).asImageBitmap()
        cache.put(key(app, px), bitmap)
        return bitmap
    }

    fun clear() = cache.evictAll()
}
