package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.CoverArt
import com.leolennards.ytdownloader.data.FetchState
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.formatDuration
import com.leolennards.ytdownloader.ui.components.ArtworkFrame
import com.leolennards.ytdownloader.ui.components.ChoicePill
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.PrimaryButton
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.SkeletonLine
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit
import com.leolennards.ytdownloader.ui.theme.ytSpring
import com.leolennards.ytdownloader.ui.theme.ytTween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val QualityOptions = listOf("1080p", "720p", "480p", "360p")
val QualityHeights = listOf(1080, 720, 480, 360)

@Composable
fun PreviewScreen(
    fetch: FetchState,
    formatIndex: Int,
    onFormatChange: (Int) -> Unit,
    qualityIndex: Int,
    onQualityChange: (Int) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    // folder name from the settings
    saveFolder: String,
    modifier: Modifier = Modifier,
) {
    val ready = fetch as? FetchState.Ready
    val failed = fetch as? FetchState.Error
    val loading = ready == null && failed == null
    val isVideo = formatIndex == 1
    val formatName = if (isVideo) "MP4" else "MP3"
    val motion = LocalMotionEnabled.current

    val thumbnail = ready?.meta?.thumbnail
    val wide: ImageBitmap? = remember(thumbnail) { thumbnail?.asImageBitmap() }
    // the same square the mp3 will get, so what you see here is what ends up in your music app
    // the official album cover when one was found, otherwise the thumbnail made square
    val official = ready?.meta?.officialCover
    val square by produceState<ImageBitmap?>(null, thumbnail, official) {
        val source = official ?: thumbnail
        value = source?.let { bmp ->
            withContext(Dispatchers.Default) { runCatching { CoverArt.squareCover(bmp).asImageBitmap() }.getOrNull() }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(
                R.drawable.ic_back,
                "Back",
                onBack,
                tint = MaterialTheme.colorScheme.onSurface,
                background = YtTheme.colors.surfaceHigh,
            )
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EyebrowLabel(
                    when {
                        ready != null -> "Ready to save"
                        failed != null -> "Not found"
                        else -> "Looking it up"
                    },
                )
            }
            Box(Modifier.size(YtDimens.MinTouch))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = YtDimens.ScreenPadding)
                .padding(top = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
        ) {
            // the picture: wide for video, the square cover for music
            AnimatedContent(
                targetState = isVideo,
                transitionSpec = {
                    (fadeIn(ytTween(motion)) + scaleIn(ytSpring(motion), initialScale = 0.94f)) togetherWith
                        fadeOut(ytTween(motion))
                },
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth(),
                label = "previewArt",
            ) { video ->
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val art = if (video) wide else (square ?: wide)
                    Box(
                        modifier = if (video) {
                            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                        } else {
                            Modifier.fillMaxWidth(0.72f).aspectRatio(1f)
                        }.shadow(16.dp, MaterialTheme.shapes.large, spotColor = Color.Black),
                        contentAlignment = Alignment.Center,
                    ) {
                        ArtworkFrame(
                            image = art,
                            format = if (video) MediaFormat.MP4 else MediaFormat.MP3,
                            modifier = Modifier.fillMaxSize(),
                            shape = MaterialTheme.shapes.large,
                            iconSize = 36.dp,
                            showIcon = !loading,
                        )
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = YtTheme.colors.track,
                                strokeWidth = 3.dp,
                            )
                        }
                        if (!video && official != null) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.72f), PillShape)
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                YtIcon(R.drawable.ic_check, tint = Color.White, size = 13.dp)
                                Text("Official album art", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                        }
                        if (ready != null && ready.meta.durationSeconds > 0) {
                            Text(
                                formatDuration(ready.meta.durationSeconds),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.72f), PillShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.animateContentSize(motionSpring()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                when {
                    ready != null -> {
                        Text(
                            ready.meta.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        if (ready.meta.uploader.isNotBlank()) {
                            Text(
                                ready.meta.uploader,
                                style = MaterialTheme.typography.bodyMedium,
                                color = YtTheme.colors.accentText,
                            )
                        }
                    }

                    failed != null -> {
                        Text(
                            "Could not load this link",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            failed.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        // stand-ins shaped like the title and channel, so the page does not jump
                        SkeletonLine(280.dp, 22.dp)
                        SkeletonLine(180.dp, 22.dp)
                        Spacer(Modifier.height(4.dp))
                        SkeletonLine(120.dp)
                        Text(
                            "Getting the details. The first lookup can take a few seconds.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = ready != null, enter = ytEnter(), exit = ytExit()) {
                Column(verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap)) {
                    SegmentedControl(FormatOptions, formatIndex, onFormatChange)

                    AnimatedVisibility(visible = isVideo, enter = ytEnter(), exit = ytExit()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "Quality",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QualityOptions.forEachIndexed { index, label ->
                                    ChoicePill(
                                        text = label,
                                        selected = index == qualityIndex,
                                        onClick = { onQualityChange(index) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                            Text(
                                "If a quality is not available, the closest lower one is saved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        YtIcon(R.drawable.ic_folder, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
                        Text(
                            buildString {
                                append("Saves to ")
                                append(if (isVideo) "Movies / $saveFolder" else "Music / $saveFolder")
                                if (!isVideo) append(if (official != null) " with the official album art" else " with cover art")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = YtDimens.ScreenPadding, vertical = 16.dp),
        ) {
            if (failed != null) {
                PrimaryButton(text = "Try again", onClick = onRetry, icon = R.drawable.ic_retry, modifier = Modifier.fillMaxWidth())
            } else {
                PrimaryButton(
                    text = if (ready == null) "Getting ready" else "Download $formatName",
                    onClick = onDownload,
                    enabled = ready != null,
                    icon = R.drawable.ic_download,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
