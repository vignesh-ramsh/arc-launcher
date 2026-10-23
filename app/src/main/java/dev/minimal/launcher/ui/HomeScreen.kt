package dev.minimal.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.data.AppInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

private const val SWIPE_UP_THRESHOLD_PX = 120f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    pinned: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onUnpin: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val now by rememberNow()
    val timeFormat = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onOpenDrawer) {
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f },
                    onVerticalDrag = { _, dy -> travelled += dy },
                    onDragEnd = { if (travelled < -SWIPE_UP_THRESHOLD_PX) onOpenDrawer() },
                )
            }
            .pointerInput(onOpenSettings) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
            .systemBarsPadding()
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            text = now.format(timeFormat),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = now.format(dateFormat),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        if (pinned.isEmpty()) {
            Text(
                text = "Swipe up for apps.\nLong-press an app there to pin it here.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            pinned.forEach { app ->
                Text(
                    text = app.label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onLaunch(app) },
                            onLongClick = { onUnpin(app) },
                        )
                        .padding(vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Current local time, refreshed at the top of each minute. */
@Composable
private fun rememberNow(): State<LocalDateTime> = produceState(LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000 - System.currentTimeMillis() % 60_000)
    }
}
