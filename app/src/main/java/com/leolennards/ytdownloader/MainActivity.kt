package com.leolennards.ytdownloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.leolennards.ytdownloader.ui.YtApp
import com.leolennards.ytdownloader.ui.theme.YTDownloaderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YTDownloaderTheme {
                YtApp()
            }
        }
    }
}
