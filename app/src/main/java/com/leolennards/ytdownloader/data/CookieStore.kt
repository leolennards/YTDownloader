package com.leolennards.ytdownloader.data

import android.app.Application
import android.net.Uri
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// keeps a copy of a cookies.txt file the user picked, for sites that need a login
// (like instagram). stored privately in the app's own files, not shared anywhere
object CookieStore {
    private lateinit var app: Application
    private val _hasCookies = MutableStateFlow(false)
    val hasCookies: StateFlow<Boolean> = _hasCookies.asStateFlow()

    fun init(application: Application) {
        app = application
        _hasCookies.value = file().length() > 0
    }

    fun file(): File = File(app.filesDir, "cookies.txt")

    // copies the picked file in, returns true if it worked
    fun import(uri: Uri): Boolean {
        return try {
            val ok = app.contentResolver.openInputStream(uri)?.use { input ->
                file().outputStream().use { output -> input.copyTo(output) }
                true
            } ?: false
            _hasCookies.value = ok && file().length() > 0
            _hasCookies.value
        } catch (e: Exception) {
            false
        }
    }

    fun clear() {
        runCatching { file().delete() }
        _hasCookies.value = false
    }
}
