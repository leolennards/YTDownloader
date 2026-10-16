package com.leolennards.ytdownloader.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.PlaybackState
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytSpring
import com.leolennards.ytdownloader.ui.theme.ytTween

// the song that is playing, floating above the nav bar. tap to open the player, swipe down to stop
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.current ?: return
    val motion = LocalMotionEnabled.current
    val progress = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, MaterialTheme.shapes.medium, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(MaterialTheme.shapes.medium)
            .background(YtTheme.colors.surfaceHigh)
            .border(YtDimens.Hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .pointerInput(Unit) {
                var drag = 0f
                detectVerticalDragGestures(
                    onDragStart = { drag = 0f },
                    onDragEnd = { if (drag > 60f) onStop() },
                ) { _, amount -> drag += amount }
            }
            .ytClickable(pressedScale = 0.98f, onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Artwork(item.uri, item.format, Modifier.size(44.dp), loadSize = 120.dp)
            }
            // the title slides when the song changes
            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (fadeIn(ytTween(motion)) + slideInVertically(ytSpring(motion)) { it / 2 }) togetherWith
                        (fadeOut(ytTween(motion)) + slideOutVertically(ytSpring(motion)) { -it / 2 })
                },
                modifier = Modifier.weight(1f),
                label = "miniTitle",
            ) { shown ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        shown.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Equalizer(state.playing, height = 10.dp)
                        Text(
                            shown.artist?.takeIf { it.isNotBlank() } ?: "Now playing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            RoundIconButton(
                if (state.playing) R.drawable.ic_pause else R.drawable.ic_play,
                if (state.playing) "Pause" else "Play",
                onClick = onToggle,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            if (state.hasNext) {
                RoundIconButton(R.drawable.ic_skip_next, "Next", onClick = onNext, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        // thin progress line along the bottom edge
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(YtTheme.colors.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(2.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
