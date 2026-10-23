package dev.minimal.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimal.launcher.ui.LauncherRoot
import dev.minimal.launcher.ui.LauncherTheme
import dev.minimal.launcher.widgets.WidgetController

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    private val widgetController by lazy {
        WidgetController(this, viewModel.widgetHost, viewModel.widgetManager) { id ->
            viewModel.addWidget(id)
            viewModel.setHomePage(1)
            viewModel.goHome()
        }
    }

    private val pickWallpaper = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) viewModel.setWallpaperFromUri(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LauncherTheme(settings) {
                LauncherRoot(
                    viewModel = viewModel,
                    onPickWallpaper = {
                        pickWallpaper.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onAddWidget = widgetController::start,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            viewModel.widgetHost.startListening()
        } catch (_: RuntimeException) {
            // The widget service is unavailable; widgets just won't live-update.
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            viewModel.widgetHost.stopListening()
        } catch (_: RuntimeException) {
            // Nothing to clean up if the widget service already died.
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!widgetController.handleResult(requestCode, resultCode)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // The Home button re-delivers the HOME intent to a singleTask launcher.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.goHome()
    }
}
