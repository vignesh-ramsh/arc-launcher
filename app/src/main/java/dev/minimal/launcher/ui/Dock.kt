package dev.minimal.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.minimal.launcher.data.AppInfo

@Composable
fun Dock(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onRemove: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        apps.forEach { app ->
            AppIcon(
                app = app,
                size = 52.dp,
                modifier = Modifier.pressable(
                    onLongClick = { onRemove(app) },
                    onClick = { onLaunch(app) },
                ),
            )
        }
    }
}
