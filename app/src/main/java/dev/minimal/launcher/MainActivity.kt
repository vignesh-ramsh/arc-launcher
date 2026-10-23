package dev.minimal.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimal.launcher.ui.LauncherRoot
import dev.minimal.launcher.ui.LauncherTheme

class MainActivity : ComponentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mode by viewModel.themeMode.collectAsStateWithLifecycle()
            LauncherTheme(mode) {
                LauncherRoot(viewModel)
            }
        }
    }

    // The Home button re-delivers the HOME intent to a singleTask launcher.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) viewModel.goHome()
    }
}
