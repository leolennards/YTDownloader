package com.leolennards.ytdownloader.data

import android.app.Application
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// the options in the settings screen
data class SettingsState(
    // start downloading right away when a link is shared to the app
    val autoDownloadShares: Boolean = false,
    // 0 = mp3, 1 = mp4
    val shareFormatIndex: Int = 0,
    // video quality for shared links (only used for mp4)
    val shareHeight: Int = 720,
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
        _state.value = SettingsState(
            autoDownloadShares = prefs.getBoolean("autoDownloadShares", false),
            shareFormatIndex = prefs.getInt("shareFormatIndex", 0),
            shareHeight = prefs.getInt("shareHeight", 720),
        )
    }

    fun update(change: (SettingsState) -> SettingsState) {
        _state.update(change)
        val s = _state.value
        app.getSharedPreferences(PREFS, Application.MODE_PRIVATE).edit()
            .putBoolean("autoDownloadShares", s.autoDownloadShares)
            .putInt("shareFormatIndex", s.shareFormatIndex)
            .putInt("shareHeight", s.shareHeight)
            .apply()
    }
}
