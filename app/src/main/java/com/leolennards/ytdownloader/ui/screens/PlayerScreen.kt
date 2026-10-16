package com.leolennards.ytdownloader.ui.screens

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.formatDuration
import com.leolennards.ytdownloader.ui.components.ArtworkCache
import com.leolennards.ytdownloader.ui.components.ArtworkFrame
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.components.ambientColor
import com.leolennards.ytdownloader.ui.theme.Motion
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
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
// VideoView can be unreliable with audio-only files on some phones ("Can't play this video").
// items is the list it was opened from, so previous and next walk through it
@Composable
fun PlayerScreen(
    items: List<DownloadItem>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = items.getOrNull(index) ?: run { onBack(); return }
    val isVideo = item.format == MediaFormat.MP4
    val uri = item.uri ?: run { onBack(); return }
    val hasPrevious = index > 0
    val hasNext = index < items.lastIndex
    val context = LocalContext.current
    val density = LocalDensity.current

    // the cover, and a soft glow of its main color behind everything
    val artPx = with(density) { 720.dp.roundToPx() }
    val art by produceState(ArtworkCache.peek(uri, artPx), uri) {
        if (value == null) value = ArtworkCache.load(context, uri, item.format, artPx)
    }
    val glowTarget = remember(art) { art?.ambientColor() } ?: MaterialTheme.colorScheme.primary
    val glow by animateColorAsState(glowTarget, motionTween(Motion.Screen), label = "playerGlow")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.38f),
                    0.65f to MaterialTheme.colorScheme.background,
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = YtDimens.ScreenPadding)
                .padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(
                    R.drawable.ic_back,
                    "Back",
                    onBack,
                    tint = MaterialTheme.colorScheme.onSurface,
                    background = YtTheme.colors.surfaceHigh.copy(alpha = 0.7f),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EyebrowLabel(if (items.size > 1) "Playing ${index + 1} of ${items.size}" else "Now playing")
                }
                Box(Modifier.size(YtDimens.MinTouch))
            }

            // a fresh player for each file
            key(uri) {
                val state = remember { PlayerState() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isVideo) {
                        VideoPlayer(uri, state)
                    } else {
                        AudioPlayer(uri, state, onFinished = { if (hasNext) onIndexChange(index + 1) })
                        val playingScale by animateFloatAsState(
                            if (state.playing) 1f else 0.9f,
                            motionSpring(dampingRatio = 0.7f),
                            label = "artScale",
                        )
                        ArtworkFrame(
                            image = art,
                            format = MediaFormat.MP3,
                            shape = MaterialTheme.shapes.large,
                            iconSize = 64.dp,
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                .graphicsLayer {
                                    scaleX = playingScale
                                    scaleY = playingScale
                                }
                                .shadow(32.dp, MaterialTheme.shapes.large, spotColor = glow),
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.artist?.takeIf { it.isNotBlank() } ?: item.meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = YtTheme.colors.accentText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                            thumbColor = MaterialTheme.colorScheme.onBackground,
                            activeTrackColor = MaterialTheme.colorScheme.onBackground,
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
                            "-" + formatDuration(((state.duration - shown).coerceAtLeast(0f) / 1000).toLong()),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkipButton(R.drawable.ic_skip_prev, "Previous", enabled = hasPrevious || state.position > 3000) {
                        // like most players: restart the song first, go back on a second press
                        if (state.position > 3000 || !hasPrevious) state.seekTo?.invoke(0) else onIndexChange(index - 1)
                    }
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .ytClickable { state.toggle?.invoke() }
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        YtIcon(
                            if (state.playing) R.drawable.ic_pause else R.drawable.ic_play,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 32.dp,
                            contentDescription = if (state.playing) "Pause" else "Play",
                        )
                    }
                    SkipButton(R.drawable.ic_skip_next, "Next", enabled = hasNext) { onIndexChange(index + 1) }
                }
            }
        }
    }
}

@Composable
private fun SkipButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.3f, motionTween(), label = "skipAlpha")
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .ytClickable(enabled = enabled, onClick = onClick)
            .graphicsLayer { this.alpha = alpha },
        contentAlignment = Alignment.Center,
    ) {
        YtIcon(icon, tint = MaterialTheme.colorScheme.onBackground, size = 28.dp, contentDescription = description)
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
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
    )
}

// plays an mp3 with MediaPlayer directly, no view needed
@Composable
private fun AudioPlayer(uri: String, state: PlayerState, onFinished: () -> Unit) {
    val context = LocalContext.current
    val finished by rememberUpdatedState(onFinished)
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
            finished()
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
