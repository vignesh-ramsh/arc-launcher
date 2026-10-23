package dev.minimal.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.ThemeMode

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    hiddenApps: List<AppInfo>,
    onUnhide: (AppInfo) -> Unit,
    onDefaultLauncher: () -> Unit,
    onSystemSettings: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val onBackground = MaterialTheme.colorScheme.onBackground
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        item {
            Label("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = mode == themeMode
                    Text(
                        text = mode.name,
                        fontSize = 22.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Light,
                        color = if (selected) onBackground else dim,
                        modifier = Modifier.clickable { onThemeMode(mode) }.padding(vertical = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            Label("System")
            Action("Default launcher", onDefaultLauncher)
            Action("Android settings", onSystemSettings)
            Spacer(Modifier.height(32.dp))

            Label("Hidden apps")
            if (hiddenApps.isEmpty()) {
                Text("None. Hide apps from the drawer with a long-press.", fontSize = 14.sp, color = dim)
            }
        }
        items(hiddenApps, key = { it.packageName }) { app ->
            Row(
                Modifier.fillMaxWidth().clickable { onUnhide(app) }.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(app.label, fontSize = 20.sp, fontWeight = FontWeight.Light, color = onBackground)
                Text("Unhide", fontSize = 14.sp, color = dim)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun Action(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}
