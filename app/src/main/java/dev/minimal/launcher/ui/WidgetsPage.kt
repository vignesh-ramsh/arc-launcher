package dev.minimal.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun WidgetsPage(
    widgetIds: List<Int>,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    val accent = LocalAccent.current
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(48.dp))
        widgetIds.forEach { id ->
            WidgetItem(
                id = id,
                host = host,
                manager = manager,
                editing = editing,
                onRemove = { onRemoveWidget(id) },
            )
        }
        if (widgetIds.isEmpty()) {
            Text("No widgets yet.", fontSize = 14.sp, color = dim, modifier = Modifier.padding(vertical = 8.dp))
        }
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Text(
                text = "Add widget",
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = accent,
                modifier = Modifier.pressable(onClick = onAddWidget).padding(vertical = 8.dp),
            )
            if (widgetIds.isNotEmpty()) {
                Text(
                    text = if (editing) "Done" else "Edit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = dim,
                    modifier = Modifier.pressable(onClick = { editing = !editing }).padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun WidgetItem(
    id: Int,
    host: AppWidgetHost,
    manager: AppWidgetManager,
    editing: Boolean,
    onRemove: () -> Unit,
) {
    val info = remember(id) { manager.getAppWidgetInfo(id) } ?: return
    val height = with(LocalDensity.current) { info.minHeight.toDp() }.coerceIn(72.dp, 420.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        AndroidView(
            factory = { context -> host.createView(context, id, info) },
            modifier = Modifier.fillMaxSize(),
        )
        if (editing) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Remove",
                    fontSize = 20.sp,
                    color = LocalAccent.current,
                    modifier = Modifier.pressable(onClick = onRemove).padding(16.dp),
                )
            }
        }
    }
}

private data class WidgetEntry(val info: AppWidgetProviderInfo, val label: String, val appLabel: String)

@Composable
fun WidgetPickerScreen(
    manager: AppWidgetManager,
    onPick: (AppWidgetProviderInfo) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val entries by produceState<List<WidgetEntry>>(emptyList()) {
        value = withContext(Dispatchers.Default) {
            val pm = context.packageManager
            manager.installedProviders
                .filter { it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0 }
                .map { info ->
                    val pkg = info.provider.packageName
                    val appLabel = try {
                        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
                    } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
                        pkg
                    }
                    WidgetEntry(info, info.loadLabel(pm) ?: pkg, appLabel)
                }
                .sortedWith(compareBy({ it.appLabel.lowercase() }, { it.label.lowercase() }))
        }
    }
    val dim = MaterialTheme.colorScheme.onSurfaceVariant

    LazyColumn(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        item {
            Text(
                text = "ADD A WIDGET",
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                color = dim,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        items(entries, key = { it.info.provider.flattenToString() }) { entry ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .pressable(onClick = { onPick(entry.info) })
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = entry.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(text = entry.appLabel, fontSize = 13.sp, color = dim)
            }
        }
    }
}
