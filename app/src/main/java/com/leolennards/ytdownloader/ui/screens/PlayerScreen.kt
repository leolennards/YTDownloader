package com.leolennards.ytdownloader.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.leolennards.ytdownloader.data.PlaybackState
import com.leolennards.ytdownloader.data.formatDuration
import com.leolennards.ytdownloader.ui.components.ArtworkCache
import com.leolennards.ytdownloader.ui.components.ArtworkFrame
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.components.ambientColor
import com.leolennards.ytdownloader.ui.components.sharedArt
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.Motion
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytTween
import kotlinx.coroutines.delay

// what the shared controls need, so music (Playback) and video (VideoView) look the same
private class Controls(
    val playing: Boolean,
    val positionMs: Int,
    val durationMs: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val onToggle: () -> Unit,
    val onSeek: (Int) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

// full screen music player. the song itself plays in Playback, so it keeps going when you leave
@Composable
fun MusicPlayerScreen(
    state: PlaybackState,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val uri = item.uri ?: return
    val context = LocalContext.current
    val artPx = with(LocalDensity.current) { 720.dp.roundToPx() }
    val art by produceState(ArtworkCache.peek(uri, artPx), uri) {
        if (value == null) value = ArtworkCache.load(context, uri, item.format, artPx)
    }
    val glow = rememberGlow(art?.let { remember(it) { it.ambientColor() } })
    val motion = LocalMotionEnabled.current

    PlayerFrame(
        glow = glow,
        eyebrow = if (state.queue.size > 1) "Playing ${state.index + 1} of ${state.queue.size}" else "Now playing",
        item = item,
        controls = Controls(
            playing = state.playing,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            hasPrevious = state.hasPrevious,
            hasNext = state.hasNext,
            onToggle = onToggle,
            onSeek = onSeek,
            onPrevious = onPrevious,
            onNext = onNext,
        ),
        onBack = onBack,
        modifier = modifier,
    ) {
        // the cover breathes a little: full size while playing, a bit smaller when paused
        val scale by animateFloatAsState(if (state.playing) 1f else 0.88f, motionSpring(dampingRatio = 0.7f), label = "artScale")
        AnimatedContent(
            targetState = item,
            transitionSpec = { fadeIn(ytTween(motion, Motion.Standard)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
            contentAlignment = Alignment.Center,
            label = "playerArt",
        ) { shown ->
            ArtworkFrame(
                image = if (shown.uri == uri) art else null,
                format = MediaFormat.MP3,
                shape = MaterialTheme.shapes.large,
                iconSize = 64.dp,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .sharedArt(shown.uri)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .shadow(32.dp, MaterialTheme.shapes.large, spotColor = glow),
            )
        }
    }
}

// full screen video player, walks through the videos it was opened from
@Composable
fun VideoPlayerScreen(
    items: List<DownloadItem>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = items.getOrNull(index) ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val uri = item.uri ?: return
    // a fresh video view for each file
    key(uri) {
        val video = remember { VideoState() }
        PlayerFrame(
            glow = rememberGlow(null),
            eyebrow = if (items.size > 1) "Video ${index + 1} of ${items.size}" else "Now playing",
            item = item,
            controls = Controls(
                playing = video.playing,
                positionMs = video.position,
                durationMs = video.duration,
                hasPrevious = index > 0,
                hasNext = index < items.lastIndex,
                onToggle = { video.toggle?.invoke() },
                onSeek = { video.seekTo?.invoke(it) },
                onPrevious = {
                    if (video.position > 3000 || index == 0) video.seekTo?.invoke(0) else onIndexChange(index - 1)
                },
                onNext = { onIndexChange(index + 1) },
            ),
            onBack = onBack,
            modifier = modifier,
        ) {
            VideoPlayer(uri, video, Modifier.sharedArt(uri), onFinished = { if (index < items.lastIndex) onIndexChange(index + 1) })
        }
    }
}

@Composable
private fun rememberGlow(target: Color?): Color {
    val glow by animateColorAsState(target ?: MaterialTheme.colorScheme.primary, motionTween(Motion.Screen), label = "playerGlow")
    return glow
}

// the layout both players share: back button, the picture, title, seek bar and buttons
@Composable
private fun PlayerFrame(
    glow: Color,
    eyebrow: String,
    item: DownloadItem,
    controls: Controls,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    media: @Composable () -> Unit,
) {
    val haptics = rememberHaptics()
    val motion = LocalMotionEnabled.current
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.42f),
                    0.7f to MaterialTheme.colorScheme.background,
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
                    onClick = onBack,
                    tint = MaterialTheme.colorScheme.onSurface,
                    background = YtTheme.colors.surfaceHigh.copy(alpha = 0.7f),
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { EyebrowLabel(eyebrow) }
                Box(Modifier.size(YtDimens.MinTouch))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) { media() }

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
                val total = controls.durationMs.coerceAtLeast(1).toFloat()
                val shown = if (dragging) dragValue else controls.positionMs.toFloat()
                Slider(
                    value = shown.coerceIn(0f, total),
                    onValueChange = {
                        dragging = true
                        dragValue = it
                    },
                    onValueChangeFinished = {
                        controls.onSeek(dragValue.toInt())
                        dragging = false
                    },
                    valueRange = 0f..total,
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
                        "-" + formatDuration(((controls.durationMs - shown).coerceAtLeast(0f) / 1000).toLong()),
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
                SkipButton(R.drawable.ic_skip_prev, "Previous", enabled = controls.hasPrevious || controls.positionMs > 3000, onClick = controls.onPrevious)
                val pressScale by animateFloatAsState(if (controls.playing) 1f else 1.06f, motionSpring(dampingRatio = 0.5f), label = "playPop")
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .graphicsLayer {
                            scaleX = pressScale
                            scaleY = pressScale
                        }
                        .clip(CircleShape)
                        .ytClickable(haptic = false) {
                            haptics.confirm()
                            controls.onToggle()
                        }
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    // play and pause swap with a quick fade and turn
                    AnimatedContent(
                        targetState = controls.playing,
                        transitionSpec = { fadeIn(ytTween(motion, Motion.Micro)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
                        label = "playIcon",
                    ) { playing ->
                        YtIcon(
                            if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            size = 32.dp,
                            contentDescription = if (playing) "Pause" else "Play",
                        )
                    }
                }
                SkipButton(R.drawable.ic_skip_next, "Next", enabled = controls.hasNext, onClick = controls.onNext)
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

// play state for the video view
private class VideoState {
    var playing by mutableStateOf(false)
    var position by mutableIntStateOf(0)
    var duration by mutableIntStateOf(0)
    var seekTo: ((Int) -> Unit)? = null
    var toggle: (() -> Unit)? = null
}

// plays an mp4 with android's built in video view
@Composable
private fun VideoPlayer(uri: String, state: VideoState, modifier: Modifier, onFinished: () -> Unit) {
    var view by remember { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(view) {
        val v = view ?: return@LaunchedEffect
        while (true) {
            state.position = v.currentPosition
            state.playing = v.isPlaying
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
                    onFinished()
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
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
    )
}
