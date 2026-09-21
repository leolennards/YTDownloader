package com.leolennards.ytdownloader

import android.app.Application
import com.leolennards.ytdownloader.data.AppSettings
import com.leolennards.ytdownloader.data.DownloadController

class YtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        DownloadController.init(this)
    }
}
