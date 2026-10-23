package dev.minimal.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.data.AppInfo

@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    pinned: Set<String>,
    docked: Set<String>,
    onLaunch: (AppInfo) -> Unit,
    onTogglePin: (AppInfo) -> Unit,
    onToggleDock: (AppInfo) -> Unit,
    onHide: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val settings = LocalSettings.current
    var query by remember { mutableStateOf("") }
    var menuFor by remember { mutableStateOf<AppInfo?>(null) }
    val filtered = remember(apps, query) {
        val needle = query.trim()
        if (needle.isEmpty()) apps else apps.filter { it.label.contains(needle, ignoreCase = true) }
    }
    val onBackground = MaterialTheme.colorScheme.onBackground
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = 32.dp),
    ) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(filtered, key = { it.packageName }) { app ->
                Box(Modifier.animateItem()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressable(onLongClick = { menuFor = app }, onClick = { onLaunch(app) })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (settings.drawerIcons) {
                            AppIcon(app, 40.dp)
                            Spacer(Modifier.width(18.dp))
                        }
                        Text(
                            text = app.label,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            color = onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = menuFor == app,
                        onDismissRequest = { menuFor = null },
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (app.packageName in pinned) "Unpin from home" else "Pin to home") },
                            onClick = { onTogglePin(app); menuFor = null },
                        )
                        DropdownMenuItem(
                            text = { Text(if (app.packageName in docked) "Remove from dock" else "Add to dock") },
                            onClick = { onToggleDock(app); menuFor = null },
                        )
                        DropdownMenuItem(
                            text = { Text("Hide") },
                            onClick = { onHide(app); menuFor = null },
                        )
                        DropdownMenuItem(
                            text = { Text("App info") },
                            onClick = { onAppInfo(app); menuFor = null },
                        )
                        DropdownMenuItem(
                            text = { Text("Uninstall") },
                            onClick = { onUninstall(app); menuFor = null },
                        )
                    }
                }
            }
        }

        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = onBackground, fontSize = 20.sp),
            cursorBrush = SolidColor(LocalAccent.current),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { filtered.firstOrNull()?.let(onLaunch) }),
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("Search", color = dim, fontSize = 20.sp)
                    inner()
                }
            },
        )
    }
}
