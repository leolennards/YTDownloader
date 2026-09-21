package com.leolennards.ytdownloader.data

import android.graphics.Bitmap
import com.leolennards.ytdownloader.R

enum class MediaFormat(val label: String, val icon: Int) {
    MP3("MP3", R.drawable.ic_music),
    MP4("MP4", R.drawable.ic_video),
}

// what a list row needs to show
data class DownloadItem(
    val title: String,
    val format: MediaFormat,
    val meta: String,
    val uri: String? = null,
    val artist: String? = null,
)

// a finished download, saved so the library is still there after restarting
data class HistoryEntry(
    val title: String,
    val format: MediaFormat,
    val qualityLabel: String?,
    val sizeBytes: Long,
    val savedAtMs: Long,
    val uri: String,
    val artist: String? = null,
)

fun HistoryEntry.toItem() = DownloadItem(
    title = title,
    format = format,
    meta = listOfNotNull(format.label, qualityLabel, formatSize(sizeBytes), relativeDay(savedAtMs))
        .joinToString(" · "),
    uri = uri,
    artist = artist,
)

class VideoMeta(
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnail: Bitmap?,
)

sealed interface InitState {
    data object Initializing : InitState
    data object Ready : InitState
    data class Failed(val message: String) : InitState
}

sealed interface FetchState {
    data object Idle : FetchState
    data object Loading : FetchState
    data class Ready(val url: String, val meta: VideoMeta) : FetchState
    data class Error(val message: String) : FetchState
}

enum class JobStep { Preparing, Downloading, Converting, Saving }

enum class JobStatus { Queued, Running, Done, Failed }

// one item in the download queue
data class QueueJob(
    val id: String,
    val url: String,
    val title: String,
    val format: MediaFormat,
    val height: Int,
    val status: JobStatus = JobStatus.Queued,
    val progress: Float = 0f,
    val step: JobStep = JobStep.Preparing,
    val error: String? = null,
    val result: DownloadItem? = null,
    val batch: Int = 0,
    // sub folder for playlists and albums, null = main Downloader folder
    val folder: String? = null,
    // album name and track number for the mp3 tags
    val album: String? = null,
    val track: Int? = null,
    // true while it waits for wifi
    val waitingForWifi: Boolean = false,
    // channel or artist name
    val artist: String? = null,
) {
    val quality: String? get() = if (format == MediaFormat.MP4) "${height}p" else null
    val isActive: Boolean get() = status == JobStatus.Queued || status == JobStatus.Running
}

// small summary of the queue for the paste screen
data class QueueSummary(val running: Int, val waiting: Int, val progress: Float)
