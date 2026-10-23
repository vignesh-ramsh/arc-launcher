package dev.minimal.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.data.Accent
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.ClockFormat
import dev.minimal.launcher.data.LauncherSettings
import dev.minimal.launcher.data.ThemeMode
import dev.minimal.launcher.data.WallpaperMode

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    onUpdate: (LauncherSettings.() -> LauncherSettings) -> Unit,
    hasWallpaperImage: Boolean,
    onPickWallpaper: () -> Unit,
    onSystemWallpaper: () -> Unit,
    hiddenApps: List<AppInfo>,
    onUnhide: (AppInfo) -> Unit,
    onDefaultLauncher: () -> Unit,
    onSystemSettings: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Section("Appearance") {
            Choice(ThemeMode.entries, settings.themeMode, { it.name }) { mode ->
                onUpdate { copy(themeMode = mode) }
            }
            Spacer(Modifier.height(12.dp))
            AccentPicker(settings.accent) { accent -> onUpdate { copy(accent = accent) } }
        }

        Section("Wallpaper") {
            Choice(
                options = WallpaperMode.entries,
                selected = settings.wallpaper,
                label = {
                    when (it) {
                        WallpaperMode.None -> "Solid"
                        WallpaperMode.System -> "System"
                        WallpaperMode.Image -> "Photo"
                    }
                },
            ) { mode ->
                if (mode == WallpaperMode.Image && !hasWallpaperImage) {
                    onPickWallpaper()
                } else {
                    onUpdate { copy(wallpaper = mode) }
                }
            }
            if (settings.wallpaper != WallpaperMode.None) {
                Spacer(Modifier.height(8.dp))
                Text("Dim", fontSize = 13.sp, color = dim)
                val accent = LocalAccent.current
                Slider(
                    value = settings.wallpaperDim,
                    onValueChange = { value -> onUpdate { copy(wallpaperDim = value) } },
                    valueRange = 0f..0.9f,
                    colors = SliderDefaults.colors(
                        thumbColor = accent,
                        activeTrackColor = accent,
                        inactiveTrackColor = dim.copy(alpha = 0.3f),
                    ),
                )
            }
            ActionRow("Choose photo", onPickWallpaper)
            ActionRow("System wallpaper picker", onSystemWallpaper)
        }

        Section("Home") {
            Label("Clock")
            Choice(
                options = ClockFormat.entries,
                selected = settings.clockFormat,
                label = {
                    when (it) {
                        ClockFormat.System -> "Auto"
                        ClockFormat.H12 -> "12-hour"
                        ClockFormat.H24 -> "24-hour"
                    }
                },
            ) { format -> onUpdate { copy(clockFormat = format) } }
            Spacer(Modifier.height(8.dp))
            Label("Alignment")
            Choice(listOf(false, true), settings.centerHome, { if (it) "Center" else "Left" }) { center ->
                onUpdate { copy(centerHome = center) }
            }
            ToggleRow("Show date", settings.showDate) { checked -> onUpdate { copy(showDate = checked) } }
        }

        Section("Drawer") {
            ToggleRow("Show icons", settings.drawerIcons) { checked -> onUpdate { copy(drawerIcons = checked) } }
        }

        Section("Behavior") {
            ToggleRow("Animations", settings.animations) { checked -> onUpdate { copy(animations = checked) } }
            ToggleRow("Haptic feedback", settings.haptics) { checked -> onUpdate { copy(haptics = checked) } }
        }

        Section("System") {
            ActionRow("Default launcher", onDefaultLauncher)
            ActionRow("Android settings", onSystemSettings)
        }

        Section("Hidden apps") {
            if (hiddenApps.isEmpty()) {
                Text("None. Hide apps from the drawer with a long-press.", fontSize = 14.sp, color = dim)
            }
            hiddenApps.forEach { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .pressable(onClick = { onUnhide(app) })
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = app.label,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text("Unhide", fontSize = 14.sp, color = dim)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    content()
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun Label(text: String) {
    Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun <T> Choice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val accent = LocalAccent.current
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        options.forEach { option ->
            val isSelected = option == selected
            val color by animateColorAsState(if (isSelected) accent else dim, label = "choice")
            Text(
                text = label(option),
                fontSize = 20.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                color = color,
                modifier = Modifier.pressable(onClick = { onSelect(option) }).padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit) {
    val dark = LocalDark.current
    val ring = MaterialTheme.colorScheme.onBackground
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Accent.entries.forEach { accent ->
            val isSelected = accent == selected
            Box(
                Modifier
                    .pressable(onClick = { onSelect(accent) })
                    .size(34.dp)
                    .then(
                        if (isSelected) Modifier.border(BorderStroke(2.dp, ring), CircleShape) else Modifier,
                    )
                    .padding(5.dp)
                    .clip(CircleShape)
                    .background(accent.color(dark)),
            )
        }
    }
}

@Composable
private fun ToggleRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = MaterialTheme.colorScheme.background,
                checkedBorderColor = accent,
            ),
        )
    }
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 20.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().pressable(onClick = onClick).padding(vertical = 10.dp),
    )
}
