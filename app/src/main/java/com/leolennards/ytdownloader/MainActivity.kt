package com.leolennards.ytdownloader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.leolennards.ytdownloader.data.AppSettings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.leolennards.ytdownloader.ui.YtApp
import com.leolennards.ytdownloader.ui.theme.YTDownloaderTheme

class MainActivity : ComponentActivity() {
    // text shared to the app from another app (like a youtube link), null when there is none
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // only read the intent on a fresh start, not after rotating the screen
        if (savedInstanceState == null) readShare(intent)
        setContent {
            val settings by AppSettings.state.collectAsState()
            val dark = when (settings.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            // status bar and nav bar icons follow the chosen theme
            LaunchedEffect(dark) {
                val bar = if (dark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = bar, navigationBarStyle = bar)
            }
            YTDownloaderTheme(darkTheme = dark) {
                YtApp(
                    sharedText = sharedText,
                    onSharedHandled = { sharedText = null },
                    onFinish = { finish() },
                )
            }
        }
    }

    // the app is already open and gets another share
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readShare(intent)
    }

    private fun readShare(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        }
    }
}
