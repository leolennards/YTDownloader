package com.leolennards.ytdownloader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.leolennards.ytdownloader.data.AppSettings
import com.leolennards.ytdownloader.data.CookieStore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.leolennards.ytdownloader.ui.YtApp
import com.leolennards.ytdownloader.ui.theme.YTDownloaderTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // text shared to the app from another app (like a youtube link), null when there is none
    private var sharedText by mutableStateOf<String?>(null)
    // a home screen shortcut asked for a screen, see res/xml/shortcuts.xml
    private var shortcut by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // only read the intent on a fresh start, not after rotating the screen
        if (savedInstanceState == null) readShare(intent)
        setContent {
            val settings by AppSettings.state.collectAsState()
            val hasCookies by CookieStore.hasCookies.collectAsState()
            val cookiesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    CoroutineScope(Dispatchers.IO).launch { CookieStore.import(uri) }
                }
            }
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
                    shortcut = shortcut,
                    onShortcutHandled = { shortcut = null },
                    onFinish = { finish() },
                    hasCookies = hasCookies,
                    onPickCookies = { cookiesLauncher.launch(arrayOf("*/*")) },
                    onClearCookies = { CookieStore.clear() },
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
        when (intent?.action) {
            Intent.ACTION_SEND -> if (intent.type == "text/plain") sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ACTION_NEW_DOWNLOAD, ACTION_OPEN_LIBRARY -> shortcut = intent.action
        }
    }

    companion object {
        const val ACTION_NEW_DOWNLOAD = "com.leolennards.ytdownloader.NEW_DOWNLOAD"
        const val ACTION_OPEN_LIBRARY = "com.leolennards.ytdownloader.OPEN_LIBRARY"
    }
}
