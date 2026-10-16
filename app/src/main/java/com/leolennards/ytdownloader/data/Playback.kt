package com.leolennards.ytdownloader.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val queue: List<DownloadItem> = emptyList(),
    val index: Int = 0,
    val playing: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
) {
    val current: DownloadItem? get() = queue.getOrNull(index)
    val hasNext: Boolean get() = index < queue.lastIndex
    val hasPrevious: Boolean get() = index > 0
}

// plays mp3s for the whole app, so music keeps going while you look around (the mini player).
// uses MediaPlayer directly since VideoView can be unreliable with audio-only files on some phones
object Playback {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: MediaPlayer? = null
    private var ready = false
    private var ticker: Job? = null
    private lateinit var appContext: Context

    fun play(context: Context, queue: List<DownloadItem>, index: Int) {
        appContext = context.applicationContext
        val playable = queue.filter { it.uri != null }
        if (playable.isEmpty()) return
        val start = index.coerceIn(0, playable.lastIndex)
        // tapping the song that is already playing just keeps it going
        val now = _state.value
        if (now.current?.uri == playable[start].uri && player != null) {
            _state.update { it.copy(queue = playable, index = start) }
            if (!now.playing) toggle()
            return
        }
        _state.value = PlaybackState(queue = playable, index = start)
        load(playable[start])
    }

    fun toggle() {
        val mp = player ?: run {
            _state.value.current?.let { load(it) }
            return
        }
        if (!ready) return
        runCatching {
            if (mp.isPlaying) {
                mp.pause()
                _state.update { it.copy(playing = false) }
            } else {
                val s = _state.value
                if (s.durationMs > 0 && mp.currentPosition >= s.durationMs - 300) mp.seekTo(0)
                mp.start()
                _state.update { it.copy(playing = true) }
                startTicker()
            }
        }
    }

    fun seekTo(ms: Int) {
        if (!ready) return
        runCatching { player?.seekTo(ms) }
        _state.update { it.copy(positionMs = ms) }
    }

    fun next() {
        val s = _state.value
        if (!s.hasNext) return
        _state.update { it.copy(index = s.index + 1, positionMs = 0, durationMs = 0) }
        load(s.queue[s.index + 1])
    }

    // like most players: restart the song first, go back on a second press
    fun previous() {
        val s = _state.value
        if (s.positionMs > 3000 || !s.hasPrevious) {
            seekTo(0)
            return
        }
        _state.update { it.copy(index = s.index - 1, positionMs = 0, durationMs = 0) }
        load(s.queue[s.index - 1])
    }

    fun stop() {
        release()
        _state.value = PlaybackState()
    }

    // a file was deleted, take it out of the queue
    fun remove(uri: String) {
        val s = _state.value
        val i = s.queue.indexOfFirst { it.uri == uri }
        if (i < 0) return
        if (i == s.index) {
            stop()
            return
        }
        _state.update { it.copy(queue = it.queue - it.queue[i], index = if (i < it.index) it.index - 1 else it.index) }
    }

    private fun load(item: DownloadItem) {
        release()
        val uri = item.uri ?: return
        val mp = MediaPlayer()
        player = mp
        mp.setOnErrorListener { _, _, _ ->
            // something went wrong playing this file. dont let it crash the app, just stop here
            ready = false
            _state.update { it.copy(playing = false) }
            true
        }
        mp.setOnPreparedListener {
            ready = true
            it.start()
            _state.update { s -> s.copy(playing = true, durationMs = it.duration, positionMs = 0) }
            startTicker()
        }
        mp.setOnCompletionListener {
            val s = _state.value
            if (s.hasNext) {
                next()
            } else {
                _state.update { it.copy(playing = false, positionMs = it.durationMs) }
            }
        }
        // setDataSource and prepareAsync can both throw if the file is missing or unreadable
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(appContext, Uri.parse(uri))
            mp.prepareAsync()
        }.onFailure { _state.update { it.copy(playing = false) } }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val mp = player ?: break
                runCatching {
                    if (ready) {
                        val playing = mp.isPlaying
                        _state.update { it.copy(positionMs = mp.currentPosition, playing = playing) }
                        if (!playing) return@launch
                    }
                }
                delay(250)
            }
        }
    }

    private fun release() {
        ticker?.cancel()
        ready = false
        player?.let { mp ->
            mp.setOnPreparedListener(null)
            mp.setOnCompletionListener(null)
            mp.setOnErrorListener(null)
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
        player = null
    }
}
