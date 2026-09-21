package com.leolennards.ytdownloader.data

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// the options in the settings screen
data class SettingsState(
    // format the paste screen starts with, 0 = mp3, 1 = mp4
    val defaultFormatIndex: Int = 0,
    // video quality used for mp4 downloads that skip the preview screen
    val defaultHeight: Int = 720,
    // start downloading right away when a link is shared to the app
    val autoDownloadShares: Boolean = false,
    // 0 = mp3, 1 = mp4
    val shareFormatIndex: Int = 0,
    // video quality for shared links (only used for mp4)
    val shareHeight: Int = 720,
    // folder inside Music and Movies where files are saved
    val folderName: String = "Downloader",
    // how many downloads can run at the same time
    val maxParallel: Int = 2,
    // only start downloads on wifi
    val wifiOnly: Boolean = false,
    // 0 = follow the phone, 1 = light, 2 = dark
    val themeMode: Int = 0,
)

// keeps the settings in SharedPreferences
object AppSettings {
    private const val PREFS = "settings"
    private lateinit var app: Application

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun init(application: Application) {
        app = application
        val prefs = app.getSharedPreferences(PREFS, Application.MODE_PRIVATE)
        val defaults = SettingsState()
        _state.value = SettingsState(
            defaultFormatIndex = prefs.getInt("defaultFormatIndex", defaults.defaultFormatIndex),
            defaultHeight = prefs.getInt("defaultHeight", defaults.defaultHeight),
            autoDownloadShares = prefs.getBoolean("autoDownloadShares", defaults.autoDownloadShares),
            shareFormatIndex = prefs.getInt("shareFormatIndex", defaults.shareFormatIndex),
            shareHeight = prefs.getInt("shareHeight", defaults.shareHeight),
            folderName = prefs.getString("folderName", defaults.folderName) ?: defaults.folderName,
            maxParallel = prefs.getInt("maxParallel", defaults.maxParallel).coerceIn(1, 3),
            wifiOnly = prefs.getBoolean("wifiOnly", defaults.wifiOnly),
            themeMode = prefs.getInt("themeMode", defaults.themeMode).coerceIn(0, 2),
        )
    }

    fun update(change: (SettingsState) -> SettingsState) {
        _state.update(change)
        val s = _state.value
        app.getSharedPreferences(PREFS, Application.MODE_PRIVATE).edit()
            .putInt("defaultFormatIndex", s.defaultFormatIndex)
            .putInt("defaultHeight", s.defaultHeight)
            .putBoolean("autoDownloadShares", s.autoDownloadShares)
            .putInt("shareFormatIndex", s.shareFormatIndex)
            .putInt("shareHeight", s.shareHeight)
            .putString("folderName", s.folderName)
            .putInt("maxParallel", s.maxParallel)
            .putBoolean("wifiOnly", s.wifiOnly)
            .putInt("themeMode", s.themeMode)
            .apply()
    }
}
