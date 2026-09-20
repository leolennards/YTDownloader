package com.leolennards.ytdownloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.leolennards.ytdownloader.data.DownloadController
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.JobStep
import com.leolennards.ytdownloader.data.QueueJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// foreground service that keeps downloads alive and shows the progress notification.
// DownloadController does the real work, this only watches the queue
// and stops itself when everything is done
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null
    private var lastUpdate = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        if (intent?.action == ACTION_CANCEL) DownloadController.cancelAll()

        // has to call startForeground quickly or android kills it
        ServiceCompat.startForeground(
            this,
            PROGRESS_ID,
            progressNotification(DownloadController.queue.value.filter { it.isActive }),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        if (watcher == null) {
            watcher = scope.launch {
                DownloadController.queue.collect { jobs ->
                    val active = jobs.filter { it.isActive }
                    if (active.isEmpty()) {
                        showResult(DownloadController.latestBatchResults())
                        finish()
                    } else {
                        // progress changes a lot, twice a second is enough for a notification
                        val now = System.currentTimeMillis()
                        if (now - lastUpdate >= 500) {
                            lastUpdate = now
                            getSystemService(NotificationManager::class.java)
                                .notify(PROGRESS_ID, progressNotification(active))
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun finish() {
        watcher?.cancel()
        watcher = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Progress of video and audio downloads"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun progressNotification(active: List<QueueJob>): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        val running = active.filter { it.status == JobStatus.Running }
        when {
            active.isEmpty() -> {
                builder.setContentTitle("Preparing download").setContentText("Starting").setProgress(0, 0, true)
            }
            active.size == 1 -> {
                val job = active.first()
                builder.setContentTitle(if (job.title == job.url) "Preparing download" else job.title)
                when {
                    job.status == JobStatus.Queued || job.step == JobStep.Preparing ->
                        builder.setContentText("Getting ready").setProgress(0, 0, true)
                    job.step == JobStep.Downloading -> {
                        val percent = (job.progress * 100).toInt()
                        builder.setContentText("Downloading · $percent%").setProgress(100, percent, false)
                    }
                    job.step == JobStep.Converting ->
                        builder.setContentText("Converting").setProgress(0, 0, true)
                    else -> builder.setContentText("Saving to your phone").setProgress(0, 0, true)
                }
            }
            else -> {
                val average = (active.sumOf { it.progress.toDouble() } / active.size * 100).toInt()
                builder.setContentTitle("${active.size} downloads")
                    .setContentText("${running.size} active · ${active.size - running.size} waiting")
                    .setProgress(100, average, false)
            }
        }

        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        builder.addAction(0, if (active.size > 1) "Cancel all" else "Cancel", cancelIntent)
        return builder.build()
    }

    private fun showResult(results: List<QueueJob>) {
        val done = results.filter { it.status == JobStatus.Done }
        val failed = results.filter { it.status == JobStatus.Failed }
        if (done.isEmpty() && failed.isEmpty()) return

        val (title, text) = when {
            done.size == 1 && failed.isEmpty() -> "Download saved" to done.first().title
            done.isEmpty() && failed.size == 1 -> "Download failed" to (failed.first().error ?: failed.first().title)
            failed.isEmpty() -> "${done.size} downloads saved" to "They are in your library."
            else -> "${done.size} saved, ${failed.size} failed" to "Open the app to see what went wrong."
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(RESULT_ID, notification)
    }

    companion object {
        const val ACTION_CANCEL = "com.leolennards.ytdownloader.CANCEL"
        private const val CHANNEL_ID = "downloads"
        private const val PROGRESS_ID = 1
        private const val RESULT_ID = 2
    }
}
