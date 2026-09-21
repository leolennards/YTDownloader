package com.leolennards.ytdownloader.data

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import com.leolennards.ytdownloader.DownloadService
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

// handles everything that uses youtubedl-android.
// its a singleton so downloads dont depend on which screen is open.
// downloads go in a queue and a few run at once (set in the settings)
object DownloadController {
    private const val TAG = "DownloadController"
    private const val QUICK_FRAGMENTS = "2"
    private const val MAX_PLAYLIST = 200
    // things like (Audio) or [Official Music Video] in a title, for yt-dlp (python regex)
    private const val MUSIC_NOISE_REGEX =
        "(?i)\\s*[(\\[]\\s*(?:official\\s+)?(?:music\\s+|lyric\\s+)?(?:audio|video|lyrics?|visuali[sz]er)\\s*[)\\]]"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var app: Application

    // how many downloads run right now, and how many are allowed (from the settings)
    private val running = MutableStateFlow(0)
    private val limit = MutableStateFlow(2)
    private val handles = ConcurrentHashMap<String, Job>()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()
    private val sequence = AtomicLong()
    private var batchCounter = 0

    private val _init = MutableStateFlow<InitState>(InitState.Initializing)
    private val _fetch = MutableStateFlow<FetchState>(FetchState.Idle)
    private val _queue = MutableStateFlow<List<QueueJob>>(emptyList())
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val _notices = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val fetchState: StateFlow<FetchState> = _fetch.asStateFlow()
    val queue: StateFlow<List<QueueJob>> = _queue.asStateFlow()
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    // one time messages for the ui, like when a playlist was read
    val notices: SharedFlow<String> = _notices.asSharedFlow()

    private var fetchJob: Job? = null

    // yt-dlp version and manual update state, shown in the settings screen
    private val _engineVersion = MutableStateFlow<String?>(null)
    private val _updating = MutableStateFlow(false)
    private val _updateMessage = MutableStateFlow<String?>(null)
    val engineVersion: StateFlow<String?> = _engineVersion.asStateFlow()
    val updating: StateFlow<Boolean> = _updating.asStateFlow()
    val updateMessage: StateFlow<String?> = _updateMessage.asStateFlow()

    fun init(application: Application) {
        app = application
        _history.value = HistoryStore.load(app)
        scope.launch {
            AppSettings.state.collect { limit.value = it.maxParallel.coerceIn(1, 3) }
        }
        scope.launch {
            try {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
            } catch (e: Exception) {
                Log.e(TAG, "youtubedl-android failed to initialise", e)
                _init.value = InitState.Failed(e.message ?: "Could not start the download engine.")
                return@launch
            }
            // youtube changes a lot and an old yt-dlp gets blocked (403 errors), so update it before
            // the first download. wait 25 seconds at most so a bad connection doesnt block the app,
            // the built in copy still works if the update fails
            val update = scope.async {
                try {
                    updateEngineBlocking()
                } catch (e: Exception) {
                    Log.w(TAG, "yt-dlp update failed", e)
                }
            }
            withTimeoutOrNull(25_000) { update.await() }
            _init.value = InitState.Ready
            refreshVersion()
        }
    }

    // nightly is the newest, stable is the fallback. returns DONE or ALREADY_UP_TO_DATE
    private fun updateEngineBlocking(): String? {
        return try {
            YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel.NIGHTLY)?.name
        } catch (e: Exception) {
            Log.w(TAG, "nightly update failed, trying stable", e)
            YoutubeDL.getInstance().updateYoutubeDL(app, YoutubeDL.UpdateChannel.STABLE)?.name
        }
    }

    private fun refreshVersion() {
        _engineVersion.value = try {
            YoutubeDL.getInstance().version(app)
        } catch (e: Exception) {
            null
        }
    }

    // the update button in the settings
    fun updateEngine() {
        if (_updating.value) return
        // swapping yt-dlp while files are downloading could break them
        if (_queue.value.any { it.isActive }) {
            _updateMessage.value = "Wait for your downloads to finish first"
            return
        }
        _updating.value = true
        _updateMessage.value = null
        scope.launch {
            try {
                _init.first { it !is InitState.Initializing }
                val result = updateEngineBlocking()
                refreshVersion()
                _updateMessage.value = when (result) {
                    "DONE" -> "Updated to the latest version"
                    "ALREADY_UP_TO_DATE" -> "Already up to date"
                    else -> "Update finished"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "manual update failed", e)
                _updateMessage.value = "Could not update: ${friendlyMessage(e).take(80)}"
            } finally {
                _updating.value = false
            }
        }
    }

    // waits until fewer downloads than the limit are running, then takes a slot
    private suspend fun takeSlot() {
        while (true) {
            var taken = false
            running.update { r ->
                taken = r < limit.value
                if (taken) r + 1 else r
            }
            if (taken) return
            combine(running, limit) { r, l -> r < l }.first { it }
        }
    }

    // true on wifi or ethernet
    private fun onWifi(): Boolean {
        val cm = app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    // holds the job back until wifi is there, if wifi only is on
    private suspend fun waitForWifi(id: String) {
        var waiting = false
        while (AppSettings.state.value.wifiOnly && !onWifi()) {
            if (!waiting) {
                waiting = true
                setWaiting(id, true)
            }
            delay(3000)
        }
        if (waiting) setWaiting(id, false)
    }

    private fun setWaiting(id: String, value: Boolean) {
        _queue.update { list -> list.map { if (it.id == id) it.copy(waitingForWifi = value) else it } }
    }

    private fun freeSlot() {
        running.update { it - 1 }
    }

    // ---- looking up one video for the preview screen ----

    fun fetch(url: String) {
        fetchJob?.cancel()
        _fetch.value = FetchState.Loading
        fetchJob = scope.launch {
            val ready = _init.first { it !is InitState.Initializing }
            if (ready is InitState.Failed) {
                _fetch.value = FetchState.Error(ready.message)
                return@launch
            }
            try {
                val info = YoutubeDL.getInstance().getInfo(singleVideo(url))
                val meta = VideoMeta(
                    title = info.title ?: "Untitled",
                    uploader = info.uploader ?: "",
                    durationSeconds = (info.duration ?: 0).toLong(),
                    thumbnail = loadThumbnail(info.thumbnail),
                )
                _fetch.value = FetchState.Ready(url, meta)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "fetch failed", e)
                _fetch.value = FetchState.Error(friendlyMessage(e))
            }
        }
    }

    // ---- queue ----

    // adds a download to the queue and returns its id right away, the download runs in the background.
    // if the title isnt known yet the job looks it up first
    fun enqueue(url: String, format: MediaFormat, height: Int, knownTitle: String? = null): String =
        enqueueMany(listOf(url to knownTitle), format, height).first()

    // adds lots of downloads at once (link + title if known). starts the service once
    fun enqueueMany(
        entries: List<Pair<String, String?>>,
        format: MediaFormat,
        height: Int,
        folder: String? = null,
        album: String? = null,
        firstTrack: Int? = null,
    ): List<String> {
        if (entries.isEmpty()) return emptyList()
        val startsNewBatch = _queue.value.none { it.isActive }
        if (startsNewBatch) batchCounter++
        val batch = batchCounter

        val jobs = entries.mapIndexed { index, (url, knownTitle) ->
            QueueJob(
                id = "job${System.currentTimeMillis()}x${sequence.incrementAndGet()}",
                url = url,
                title = knownTitle ?: url,
                format = format,
                height = height,
                step = if (knownTitle == null) JobStep.Preparing else JobStep.Downloading,
                batch = batch,
                folder = folder,
                album = album,
                track = firstTrack?.plus(index),
            )
        }
        _queue.update { list ->
            // old finished jobs are just clutter now
            val kept = if (startsNewBatch) list.filter { it.status == JobStatus.Failed || it.batch == batch } else list
            kept + jobs
        }
        startService()
        jobs.forEachIndexed { index, job ->
            val knownTitle = entries[index].second
            handles[job.id] = scope.launch {
                waitForWifi(job.id)
                takeSlot()
                try {
                    runJob(job, knownTitle)
                } finally {
                    freeSlot()
                }
            }
        }
        return jobs.map { it.id }
    }

    // reads a playlist link and queues every video in it. it only lists them, no downloading yet,
    // so its quick. the result goes out through notices
    fun enqueuePlaylist(url: String, format: MediaFormat, height: Int) {
        scope.launch {
            val engine = _init.first { it !is InitState.Initializing }
            if (engine is InitState.Failed) {
                _notices.tryEmit("Could not start: ${engine.message.take(80)}")
                return@launch
            }
            try {
                val request = YoutubeDLRequest(url)
                request.addOption("--flat-playlist")
                request.addOption("--playlist-end", MAX_PLAYLIST.toString())
                request.addOption("--print", "%(url)s\t%(title)s\t%(playlist_title)s")
                val output = YoutubeDL.getInstance().execute(request).out

                var playlistTitle: String? = null
                val entries = output.lines().mapNotNull { line ->
                    val parts = line.split("\t")
                    val link = parts.getOrNull(0)?.trim().orEmpty()
                    if (!link.startsWith("http")) return@mapNotNull null
                    val title = parts.getOrNull(1)?.trim().orEmpty()
                    if (title.startsWith("[Private") || title.startsWith("[Deleted")) return@mapNotNull null
                    if (playlistTitle == null) {
                        playlistTitle = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() && it != "NA" }
                    }
                    link to title.takeIf { it.isNotBlank() && it != "NA" }
                }.distinctBy { it.first }

                if (entries.isEmpty()) {
                    _notices.tryEmit("No videos found in that playlist")
                    return@launch
                }
                val isAlbum = url.contains("list=OLAK5uy") || playlistTitle?.startsWith("Album - ") == true
                val cleanTitle = playlistTitle?.removePrefix("Album - ")?.trim()?.takeIf { it.isNotBlank() }
                enqueueMany(
                    entries,
                    format,
                    height,
                    folder = cleanTitle?.let { sanitize(it) },
                    album = if (isAlbum) cleanTitle else null,
                    firstTrack = if (isAlbum) 1 else null,
                )
                val noun = if (format == MediaFormat.MP3) "tracks" else "videos"
                val name = (playlistTitle?.removePrefix("Album - "))?.let { " from ${it.take(28)}" }.orEmpty()
                val capped = if (entries.size >= MAX_PLAYLIST) " (first $MAX_PLAYLIST)" else ""
                _notices.tryEmit("Added ${entries.size} $noun$name$capped")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "playlist failed", e)
                _notices.tryEmit("Could not read that playlist: ${friendlyMessage(e).take(80)}")
            }
        }
    }

    fun cancel(id: String) {
        cancelled.add(id)
        handles.remove(id)?.cancel()
        _queue.update { list -> list.filterNot { it.id == id } }
        scope.launch {
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (e: Exception) {
                Log.w(TAG, "cancel failed", e)
            }
        }
    }

    fun cancelAll() {
        _queue.value.filter { it.isActive }.forEach { cancel(it.id) }
    }

    fun retry(id: String) {
        val job = _queue.value.firstOrNull { it.id == id } ?: return
        dismiss(id)
        val title = job.title.takeIf { it != job.url }
        enqueueMany(listOf(job.url to title), job.format, job.height, job.folder, job.album, job.track)
    }

    // removes a finished or failed job from the list
    fun dismiss(id: String) {
        _queue.update { list -> list.filterNot { it.id == id } }
    }

    // finished jobs from the latest batch, used for the all done notification
    fun latestBatchResults(): List<QueueJob> =
        _queue.value.filter { it.batch == batchCounter && (it.status == JobStatus.Done || it.status == JobStatus.Failed) }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
        } catch (e: Exception) {
            Log.w(TAG, "could not start the foreground service", e)
        }
    }

    private fun update(id: String, change: (QueueJob) -> QueueJob) {
        _queue.update { list -> list.map { if (it.id == id) change(it) else it } }
    }

    private suspend fun runJob(job: QueueJob, knownTitle: String?) {
        val id = job.id
        val url = job.url
        val format = job.format
        val height = job.height
        val dir = File(app.cacheDir, "downloads").apply { mkdirs() }
        update(id) { it.copy(status = JobStatus.Running) }
        try {
            val engine = _init.first { it !is InitState.Initializing }
            if (engine is InitState.Failed) error(engine.message)

            val rawTitle = knownTitle ?: (YoutubeDL.getInstance().getInfo(singleVideo(url)).title ?: "Untitled")
            // for music, drop the "Artist - " at the start and stuff like (Audio), the artist is already in the tags
            val title = if (format == MediaFormat.MP3) cleanMusicTitle(rawTitle) else rawTitle
            update(id) { it.copy(title = title, step = JobStep.Downloading) }

            val request = YoutubeDLRequest(url)
            request.addOption("--no-playlist")
            request.addOption("-o", File(dir, "$id.%(ext)s").absolutePath)
            // download a few fragments at once, much faster on youtube
            request.addOption("--concurrent-fragments", QUICK_FRAGMENTS)
            // youtube links are tied to the ip, and phones can switch between ipv4 and ipv6 in the
            // middle of a download which gives 403 errors, so stick to ipv4
            request.addOption("--force-ipv4")
            request.addOption("--retries", "5")
            request.addOption("--fragment-retries", "5")
            if (format == MediaFormat.MP3) {
                request.addOption("-f", "bestaudio/best")
                request.addOption("-x")
                request.addOption("--audio-format", "mp3")
                request.addOption("--audio-quality", "0")
                // tags and cover art so it looks right in a music player
                request.addOption("--embed-metadata")
                request.addOption("--embed-thumbnail")
                // youtube thumbnails are often wide with black bars, so crop the cover to a square in the middle.
                // png because yt-dlp only converts (and so only crops) when the format changes
                request.addOption("--convert-thumbnails", "png")
                request.addOption(
                    "--postprocessor-args",
                    "ThumbnailsConvertor+FFmpeg_o:-c:v png -vf crop=\"'min(iw,ih)':'min(iw,ih)'\",scale=600:600",
                )
                // same clean up for the title tag, and use the "Artist - " part as the artist tag when there is one
                request.addCommands(
                    listOf(
                        "--replace-in-metadata", "title", MUSIC_NOISE_REGEX, "",
                        "--parse-metadata", "title:(?P<meta_artist>.+?) - (?P<meta_title>.+)",
                    ),
                )
                job.track?.let {
                    request.addOption("--parse-metadata", "%(id&$it|$it)s:%(meta_track)s")
                }
                job.album?.let { album ->
                    val safe = album.replace(Regex("[%()|&:\\\\]"), " ").trim()
                    if (safe.isNotBlank()) {
                        request.addOption("--parse-metadata", "%(id&$safe|$safe)s:%(meta_album)s")
                    }
                }
            } else {
                request.addOption(
                    "-f",
                    "bv*[height<=$height][ext=mp4]+ba[ext=m4a]/b[height<=$height][ext=mp4]/bv*[height<=$height]+ba/b[height<=$height]/b",
                )
                request.addOption("--merge-output-format", "mp4")
            }

            // video and audio are two downloads so the percent goes 0-100 twice
            val streams = if (format == MediaFormat.MP4) 2 else 1
            var streamIndex = 0
            var lastPercent = 0f
            // sometimes a download fails with an http error (like 403).
            // trying again a bit later usually works so do it automatically
            var attempt = 0
            while (true) {
                try {
                    streamIndex = 0
                    lastPercent = 0f
                    YoutubeDL.getInstance().execute(request, id) { percent, _, line ->
                        if (percent >= 0f) {
                            if (percent < lastPercent - 50f) streamIndex++
                            lastPercent = percent
                        }
                        val overall = ((streamIndex + lastPercent / 100f) / streams).coerceIn(0f, 0.99f)
                        val converting = line.contains("[Merger]") ||
                            line.contains("[ExtractAudio]") ||
                            line.contains("[VideoConvertor]")
                        update(id) { job ->
                            job.copy(
                                progress = overall,
                                step = if (converting) JobStep.Converting else job.step,
                            )
                        }
                    }
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    attempt++
                    val transient = e.message.orEmpty().let {
                        it.contains("HTTP", ignoreCase = true) || it.contains("timed out", ignoreCase = true) ||
                            it.contains("connection", ignoreCase = true)
                    }
                    if (id in cancelled || attempt >= 4 || !transient) throw e
                    Log.w(TAG, "download attempt $attempt failed, retrying", e)
                    update(id) { it.copy(progress = 0f, step = JobStep.Preparing) }
                    // wait longer each time, and go slower (one connection) since too many at once can get blocked
                    delay(3000L * attempt)
                    request.addOption("--concurrent-fragments", "1")
                    update(id) { it.copy(step = JobStep.Downloading) }
                }
            }

            val output = dir.listFiles { f ->
                f.name.startsWith("$id.") && !f.name.endsWith(".part") && !f.name.endsWith(".ytdl")
            }?.maxByOrNull { it.length() } ?: error("The download finished but no file was produced.")

            update(id) { it.copy(progress = 0.99f, step = JobStep.Saving) }
            val size = output.length()
            val uri = saveToMediaStore(output, title, format, job.folder)

            val quality = if (format == MediaFormat.MP4) "${height}p" else null
            val entry = HistoryEntry(title, format, quality, size, System.currentTimeMillis(), uri.toString())
            val updated = listOf(entry) + _history.value
            _history.value = updated
            HistoryStore.save(app, updated)
            update(id) { it.copy(status = JobStatus.Done, progress = 1f, result = entry.toItem()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (id !in cancelled) {
                Log.e(TAG, "download failed", e)
                update(id) { it.copy(status = JobStatus.Failed, error = friendlyMessage(e)) }
            }
        } finally {
            dir.listFiles { f -> f.name.startsWith("$id.") }?.forEach { it.delete() }
            handles.remove(id)
            cancelled.remove(id)
        }
    }

    private fun singleVideo(url: String) = YoutubeDLRequest(url).apply { addOption("--no-playlist") }

    private fun saveToMediaStore(file: File, title: String, format: MediaFormat, folder: String?): Uri {
        val isAudio = format == MediaFormat.MP3
        val displayName = sanitize(title) + "." + file.extension
        val mime = if (isAudio) "audio/mpeg" else "video/mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = app.contentResolver
            val collection = if (isAudio) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    (if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES) + "/" + baseFolder() + (folder?.let { "/$it" } ?: ""),
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values) ?: error("Could not create the file on your phone.")
            val out = resolver.openOutputStream(uri) ?: error("Could not write the file on your phone.")
            out.use { stream -> file.inputStream().use { it.copyTo(stream) } }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }

        // android 8 and 9, use the app folder so no storage permission is needed
        val target = File(
            app.getExternalFilesDir(if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES),
            baseFolder() + (folder?.let { "/$it" } ?: ""),
        ).apply { mkdirs() }
        val dest = File(target, displayName)
        file.copyTo(dest, overwrite = true)
        return Uri.fromFile(dest)
    }

    private fun loadThumbnail(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            null
        }
    }

    // main folder inside Music and Movies, from the settings
    private fun baseFolder(): String {
        val raw = AppSettings.state.value.folderName.trim()
        return if (raw.isBlank()) "Downloader" else sanitize(raw).trim('.', ' ').ifBlank { "Downloader" }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().take(120).ifBlank { "video" }

    private fun friendlyMessage(e: Exception): String {
        val message = e.message.orEmpty()
        val errorLine = message.lines().lastOrNull { it.contains("ERROR", ignoreCase = true) }
        return (errorLine ?: message).trim().ifBlank { "Something went wrong." }.take(300)
    }
}
