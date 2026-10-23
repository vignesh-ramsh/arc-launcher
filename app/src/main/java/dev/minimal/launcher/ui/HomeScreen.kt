package dev.minimal.launcher.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.minimal.launcher.data.AppInfo
import dev.minimal.launcher.data.ClockFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay

private const val SWIPE_THRESHOLD_PX = 120f
private const val PAGE_COUNT = 2

@Composable
fun HomeScreen(
    pinned: List<AppInfo>,
    dock: List<AppInfo>,
    widgetIds: List<Int>,
    widgetHost: AppWidgetHost,
    widgetManager: AppWidgetManager,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onUnpin: (AppInfo) -> Unit,
    onRemoveFromDock: (AppInfo) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onPullDown: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { PAGE_COUNT }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { onPageChanged(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(onOpenDrawer, onPullDown) {
                var travelled = 0f
                detectVerticalDragGestures(
                    onDragStart = { travelled = 0f },
                    onVerticalDrag = { _, dy -> travelled += dy },
                    onDragEnd = {
                        when {
                            travelled < -SWIPE_THRESHOLD_PX -> onOpenDrawer()
                            travelled > SWIPE_THRESHOLD_PX -> onPullDown()
                        }
                    },
                )
            }
            .pointerInput(onOpenSettings) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            }
            .systemBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            if (page == 0) {
                ClockPage(
                    pinned = pinned,
                    dockEmpty = dock.isEmpty(),
                    onLaunch = onLaunch,
                    onUnpin = onUnpin,
                )
            } else {
                WidgetsPage(
                    widgetIds = widgetIds,
                    host = widgetHost,
                    manager = widgetManager,
                    onAddWidget = onAddWidget,
                    onRemoveWidget = onRemoveWidget,
                )
            }
        }
        PageDots(count = PAGE_COUNT, current = pagerState.currentPage)
        Dock(apps = dock, onLaunch = onLaunch, onRemove = onRemoveFromDock)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ClockPage(
    pinned: List<AppInfo>,
    dockEmpty: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onUnpin: (AppInfo) -> Unit,
) {
    val settings = LocalSettings.current
    val now by rememberNow()
    val timeFormat = remember(settings.clockFormat) {
        when (settings.clockFormat) {
            ClockFormat.System -> DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            ClockFormat.H12 -> DateTimeFormatter.ofPattern("h:mm a")
            ClockFormat.H24 -> DateTimeFormatter.ofPattern("HH:mm")
        }
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    val timeText = now.format(timeFormat)
    val horizontal = if (settings.centerHome) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (settings.centerHome) TextAlign.Center else TextAlign.Start

    Column(Modifier.fillMaxSize(), horizontalAlignment = horizontal) {
        Spacer(Modifier.height(48.dp))
        AnimatedContent(
            targetState = timeText,
            transitionSpec = {
                if (settings.animations) {
                    (slideInVertically { -it / 3 } + fadeIn()) togetherWith
                        (slideOutVertically { it / 3 } + fadeOut())
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "clock",
        ) { text ->
            Text(
                text = text,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (settings.showDate) {
            Text(
                text = now.format(dateFormat),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))

        if (pinned.isEmpty() && dockEmpty) {
            Text(
                text = "Swipe up for apps.\nLong-press an app there to pin it or add it to the dock.",
                fontSize = 14.sp,
                textAlign = textAlign,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.animateContentSize(),
            horizontalAlignment = horizontal,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            pinned.forEach { app ->
                Text(
                    text = app.label,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = textAlign,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(onLongClick = { onUnpin(app) }, onClick = { onLaunch(app) })
                        .padding(vertical = 10.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val accent = LocalAccent.current
    val dim = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 18.dp else 6.dp, label = "dotWidth")
            val color by animateColorAsState(
                if (selected) accent else dim.copy(alpha = 0.4f),
                label = "dotColor",
            )
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
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
