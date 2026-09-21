package com.leolennards.ytdownloader.ui.screens

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

// plays a saved file, video view handles both mp3 and mp4
@Composable
fun PlayerScreen(item: DownloadItem, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val isVideo = item.format == MediaFormat.MP4
    var view by remember { mutableStateOf<VideoView?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    // keeps the slider moving
    LaunchedEffect(view) {
        val v = view ?: return@LaunchedEffect
        while (true) {
            if (!dragging) {
                position = v.currentPosition
                playing = v.isPlaying
            }
            delay(250)
        }
    }
    // stop when leaving the screen
    DisposableEffect(Unit) {
        onDispose { view?.stopPlayback() }
    }

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

        // the video, or just a small hidden view for audio
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(Uri.parse(item.uri))
                    setOnPreparedListener {
                        duration = it.duration
                        start()
                        playing = true
                    }
                    setOnCompletionListener {
                        playing = false
                        position = duration
                    }
                    view = this
                }
            },
            modifier = if (isVideo) {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(YtDimens.CardRadius))
                    .background(Color.Black)
            } else {
                Modifier.size(1.dp)
            },
        )
        if (!isVideo) {
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
            val shown = if (dragging) dragValue else position.toFloat()
            Slider(
                value = shown.coerceIn(0f, duration.coerceAtLeast(1).toFloat()),
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    view?.seekTo(dragValue.toInt())
                    position = dragValue.toInt()
                    dragging = false
                },
                valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
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
                    formatDuration((duration / 1000).toLong()),
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
                    .ytClickable {
                        val v = view ?: return@ytClickable
                        if (v.isPlaying) {
                            v.pause()
                            playing = false
                        } else {
                            // start again from the top if it finished
                            if (duration > 0 && v.currentPosition >= duration - 300) v.seekTo(0)
                            v.start()
                            playing = true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                YtIcon(
                    if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    size = 30.dp,
                    contentDescription = if (playing) "Pause" else "Play",
                )
            }
        }
    }
}
