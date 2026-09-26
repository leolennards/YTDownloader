package com.leolennards.ytdownloader.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.formatDuration
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.ytClickable
import kotlinx.coroutines.delay

// simple play/pause/seek state shared by both the video and the audio player below
private class PlayerState {
    var playing by mutableStateOf(false)
    var position by mutableIntStateOf(0)
    var duration by mutableIntStateOf(0)
    var dragging by mutableStateOf(false)
    var dragValue by mutableFloatStateOf(0f)
    var seekTo: ((Int) -> Unit)? = null
    var toggle: (() -> Unit)? = null
}

// plays a saved file. video uses VideoView, audio uses MediaPlayer directly since
// VideoView can be unreliable with audio-only files on some phones ("Can't play this video")
@Composable
fun PlayerScreen(item: DownloadItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val isVideo = item.format == MediaFormat.MP4
    val uri = item.uri ?: run { onBack(); return }
    val state = remember { PlayerState() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = YtDimens.ScreenPadding)
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(R.drawable.ic_back, "Back", onBack)
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        if (isVideo) {
            VideoPlayer(uri, state)
        } else {
            AudioPlayer(uri, state)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.6f)
                    .clip(RoundedCornerShape(YtDimens.CardRadius))
                    .background(YtTheme.colors.track),
                contentAlignment = Alignment.Center,
            ) {
                YtIcon(R.drawable.ic_music, tint = MaterialTheme.colorScheme.primary, size = 56.dp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val shown = if (state.dragging) state.dragValue else state.position.toFloat()
            Slider(
                value = shown.coerceIn(0f, state.duration.coerceAtLeast(1).toFloat()),
                onValueChange = {
                    state.dragging = true
                    state.dragValue = it
                },
                onValueChangeFinished = {
                    state.seekTo?.invoke(state.dragValue.toInt())
                    state.position = state.dragValue.toInt()
                    state.dragging = false
                },
                valueRange = 0f..state.duration.coerceAtLeast(1).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = YtTheme.colors.track,
                ),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    formatDuration((shown / 1000).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatDuration((state.duration / 1000).toLong()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .ytClickable { state.toggle?.invoke() },
                contentAlignment = Alignment.Center,
            ) {
                YtIcon(
                    if (state.playing) R.drawable.ic_pause else R.drawable.ic_play,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 30.dp,
                    contentDescription = if (state.playing) "Pause" else "Play",
                )
            }
        }
    }
}

// plays an mp4 with android's built in video view
@Composable
private fun VideoPlayer(uri: String, state: PlayerState) {
    var view by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(view) {
        val v = view ?: return@LaunchedEffect
        while (true) {
            if (!state.dragging) {
                state.position = v.currentPosition
                state.playing = v.isPlaying
            }
            delay(250)
        }
    }
    DisposableEffect(Unit) {
        onDispose { view?.stopPlayback() }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.parse(uri))
                setOnPreparedListener {
                    state.duration = it.duration
                    start()
                    state.playing = true
                }
                setOnCompletionListener {
                    state.playing = false
                    state.position = state.duration
                }
                state.seekTo = { pos -> seekTo(pos) }
                state.toggle = {
                    if (isPlaying) {
                        pause()
                        state.playing = false
                    } else {
                        if (state.duration > 0 && currentPosition >= state.duration - 300) seekTo(0)
                        start()
                        state.playing = true
                    }
                }
                view = this
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(YtDimens.CardRadius))
            .background(Color.Black),
    )
}

// plays an mp3 with MediaPlayer directly, no view needed
@Composable
private fun AudioPlayer(uri: String, state: PlayerState) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(uri) {
        val mp = MediaPlayer()
        // set once the file is actually ready to play, so we dont call start/pause/seek too early
        var ready = false
        mp.setOnErrorListener { _, _, _ ->
            // something went wrong playing this file. dont let it crash the app, just stop here
            ready = false
            state.playing = false
            true
        }
        mp.setOnPreparedListener {
            ready = true
            state.duration = it.duration
            it.start()
            state.playing = true
        }
        mp.setOnCompletionListener {
            state.playing = false
            state.position = state.duration
        }
        // setDataSource and prepareAsync can both throw if the file is missing or unreadable
        runCatching {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            mp.setDataSource(context, Uri.parse(uri))
            mp.prepareAsync()
        }
        state.seekTo = { pos -> if (ready) runCatching { mp.seekTo(pos) } }
        state.toggle = {
            if (ready) {
                runCatching {
                    if (mp.isPlaying) {
                        mp.pause()
                        state.playing = false
                    } else {
                        if (state.duration > 0 && mp.currentPosition >= state.duration - 300) mp.seekTo(0)
                        mp.start()
                        state.playing = true
                    }
                }
            }
        }
        player = mp
        onDispose {
            mp.setOnPreparedListener(null)
            mp.setOnCompletionListener(null)
            mp.setOnErrorListener(null)
            runCatching { if (ready) mp.stop() }
            runCatching { mp.release() }
        }
    }

    LaunchedEffect(player) {
        val mp = player ?: return@LaunchedEffect
        while (true) {
            if (!state.dragging) {
                runCatching {
                    state.position = mp.currentPosition
                    state.playing = mp.isPlaying
                }
            }
            delay(250)
        }
    }
}
