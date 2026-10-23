package dev.minimal.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import dev.minimal.launcher.data.AppIcons
import dev.minimal.launcher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(app: AppInfo, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val icon by produceState<ImageBitmap?>(AppIcons.peek(app, px), app, px) {
        if (value == null) {
            value = withContext(Dispatchers.Default) { AppIcons.load(context, app, px) }
        }
    }
    Box(modifier.size(size)) {
        icon?.let { Image(bitmap = it, contentDescription = app.label, modifier = Modifier.fillMaxSize()) }
    }
}
