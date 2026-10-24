package com.leolennards.ytdownloader.ui.screens

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.widget.VideoView
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.leolennards.ytdownloader.data.Lyrics
import com.leolennards.ytdownloader.data.LyricsFinder
import com.leolennards.ytdownloader.data.LyricsLoader
import com.leolennards.ytdownloader.ui.components.IconTile
import com.leolennards.ytdownloader.ui.components.SkeletonLine
import com.leolennards.ytdownloader.ui.theme.OnAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.leolennards.ytdownloader.ui.theme.PillShape
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
    // lyrics shown instead of the cover
    lyricsOpen: Boolean = false,
    onToggleLyrics: () -> Unit = {},
    // look lyrics up online when the song has none yet
    lyricsOnline: Boolean = true,
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
    // the words of the song: loading, then found or not. only looked up once the lyrics are opened
    val lyrics by produceState<LyricsState>(LyricsState.Loading, uri, lyricsOpen) {
        if (!lyricsOpen && value is LyricsState.Loading) return@produceState
        if (value !is LyricsState.Loading) return@produceState
        val found = withContext(Dispatchers.IO) {
            LyricsLoader.load(context, uri, item.title, item.artist, state.durationMs.toLong(), lyricsOnline)
        }
        value = LyricsState.Done(found)
    }

    PlayerFrame(
        glow = glow,
        backdrop = art,
        trailing = {
            val tint by animateColorAsState(
                if (lyricsOpen) OnAccent else MaterialTheme.colorScheme.onSurface,
                motionTween(),
                label = "lyricsButton",
            )
            RoundIconButton(
                R.drawable.ic_lyrics,
                if (lyricsOpen) "Hide lyrics" else "Show lyrics",
                onClick = onToggleLyrics,
                tint = tint,
                background = if (lyricsOpen) MaterialTheme.colorScheme.primary else YtTheme.colors.surfaceHigh.copy(alpha = 0.7f),
            )
        },
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
        AnimatedContent(
            targetState = lyricsOpen,
            transitionSpec = { fadeIn(ytTween(motion, Motion.Standard)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
            label = "coverOrLyrics",
        ) { showLyrics ->
        if (showLyrics) {
            LyricsView(lyrics, state.positionMs.toLong(), onSeek = { onSeek(it.toInt()) })
        } else {
        // the cover breathes a little: full size while playing, a bit smaller when paused
        val scale by animateFloatAsState(if (state.playing) 1f else 0.88f, motionSpring(dampingRatio = 0.7f), label = "artScale")
        // the biggest square that fits, so it never spills past the screen edges
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth * 0.92f, maxHeight)
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
                    .size(side)
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
        }
    }
}

private sealed interface LyricsState {
    data object Loading : LyricsState
    data class Done(val lyrics: Lyrics?) : LyricsState
}

// the words of the song. synced lyrics follow the music: the line being sung is bright, the
// rest are dimmed, and the list scrolls to keep it in view. tap a line to jump to it
@Composable
private fun LyricsView(state: LyricsState, positionMs: Long, onSeek: (Long) -> Unit) {
    val motion = LocalMotionEnabled.current
    val fade = Modifier
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            // soft edges at the top and bottom so lines fade in and out instead of being cut
            drawRect(
                Brush.verticalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.85f to Color.Black, 1f to Color.Transparent),
                blendMode = BlendMode.DstIn,
            )
        }
    when (state) {
        LyricsState.Loading -> Column(
            Modifier.fillMaxSize().padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            listOf(260, 200, 280, 170, 240).forEach { SkeletonLine(it.dp, 26.dp) }
        }

        is LyricsState.Done -> {
            val lyrics = state.lyrics
            val lines = lyrics?.synced
            when {
                lyrics == null -> Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IconTile(R.drawable.ic_lyrics, size = 56.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("No lyrics found", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        "This song is not in the lyrics library yet, or it is instrumental.",
                        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
                    )
                }

                lines.isNullOrEmpty() -> Text(
                    lyrics.plain,
                    style = MaterialTheme.typography.titleLarge.copy(lineHeight = 34.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(fade)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 40.dp),
                )

                else -> {
                    val current = LyricsFinder.currentLine(lines, positionMs + 250)
                    val list = rememberLazyListState()
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val offset = with(LocalDensity.current) { (maxHeight * 0.3f).roundToPx() }
                        // keep the line being sung about a third of the way down
                        LaunchedEffect(current) {
                            val target = current.coerceAtLeast(0)
                            if (motion) list.animateScrollToItem(target, -offset) else list.scrollToItem(target, -offset)
                        }
                        LazyColumn(
                            state = list,
                            modifier = Modifier.fillMaxSize().then(fade),
                            contentPadding = PaddingValues(top = maxHeight * 0.3f, bottom = maxHeight * 0.6f),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            itemsIndexed(lines) { index, line ->
                                val alpha by animateFloatAsState(
                                    when {
                                        index == current -> 1f
                                        index < current -> 0.32f
                                        else -> 0.45f
                                    },
                                    motionTween(Motion.Standard),
                                    label = "lyricAlpha",
                                )
                                Text(
                                    line.text.ifEmpty { "♪" },
                                    style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 32.sp),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer { this.alpha = alpha }
                                        .clip(MaterialTheme.shapes.small)
                                        .ytClickable(haptic = true, pressedScale = 0.98f) { onSeek(line.timeMs) }
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
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
    // the cover, shown huge and blurred behind everything (android 12 and newer)
    backdrop: ImageBitmap? = null,
    // a button at the top right, like the lyrics button
    trailing: (@Composable () -> Unit)? = null,
    media: @Composable () -> Unit,
) {
    val haptics = rememberHaptics()
    val motion = LocalMotionEnabled.current
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.42f),
                    0.7f to background,
                ),
            ),
    ) {
        if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AnimatedContent(
                targetState = backdrop,
                transitionSpec = { fadeIn(ytTween(motion, Motion.Screen)) togetherWith fadeOut(ytTween(motion, Motion.Screen)) },
                label = "backdrop",
            ) { image ->
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.3f
                            scaleY = 1.3f
                            alpha = 0.75f
                        }
                        .blur(70.dp, BlurredEdgeTreatment.Unbounded),
                )
            }
            // fades the picture into the page so the text stays easy to read
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to background.copy(alpha = 0.25f),
                            0.45f to background.copy(alpha = 0.55f),
                            0.8f to background.copy(alpha = 0.95f),
                            1f to background,
                        ),
                    ),
            )
        }
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
                if (trailing != null) trailing() else Box(Modifier.size(YtDimens.MinTouch))
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
                SeekBar(
                    fraction = (shown / total).coerceIn(0f, 1f),
                    dragging = dragging,
                    onDrag = { f ->
                        dragging = true
                        dragValue = f * total
                    },
                    onRelease = {
                        controls.onSeek(dragValue.toInt())
                        dragging = false
                    },
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

// a slim seek bar: gradient fill, a dot that grows while you drag, tap anywhere to jump there
@Composable
private fun SeekBar(fraction: Float, dragging: Boolean, onDrag: (Float) -> Unit, onRelease: () -> Unit) {
    val haptics = rememberHaptics()
    val dot by animateFloatAsState(if (dragging) 1f else 0f, motionSpring(dampingRatio = 0.6f), label = "seekDot")
    val trackColor = YtTheme.colors.track
    val brand = YtTheme.brandHorizontal
    val thumb = MaterialTheme.colorScheme.onBackground
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        haptics.tick()
                        onDrag((offset.x / size.width).coerceIn(0f, 1f))
                        tryAwaitRelease()
                        onRelease()
                    },
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = onRelease,
                    onDragCancel = onRelease,
                ) { change, _ -> onDrag((change.position.x / size.width).coerceIn(0f, 1f)) }
            }
            .semantics { contentDescription = "Seek" },
        contentAlignment = Alignment.CenterStart,
    ) {
        val height = 4.dp + 2.dp * dot
        Box(Modifier.fillMaxWidth().height(height).background(trackColor, PillShape))
        Box(Modifier.fillMaxWidth(fraction).height(height).background(brand, PillShape))
        val dotSize = 12.dp + 8.dp * dot
        Box(
            Modifier
                .offset(x = (maxWidth - dotSize) * fraction)
                .size(dotSize)
                .shadow(4.dp, PillShape)
                .background(thumb, PillShape),
        )
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
