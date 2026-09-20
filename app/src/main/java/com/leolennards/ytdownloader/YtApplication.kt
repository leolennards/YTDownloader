package com.leolennards.ytdownloader

import android.app.Application
import com.leolennards.ytdownloader.data.DownloadController

class YtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DownloadController.init(this)
    }
}
