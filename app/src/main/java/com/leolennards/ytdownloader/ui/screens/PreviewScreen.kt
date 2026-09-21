package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.FetchState
import com.leolennards.ytdownloader.data.formatDuration
import com.leolennards.ytdownloader.ui.components.ChoicePill
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.GoldButton
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

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

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = YtDimens.ScreenPadding)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(YtDimens.MinTouch)
                        .background(MaterialTheme.colorScheme.surface, PillShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    YtIcon(R.drawable.ic_back, contentDescription = "Back")
                }
                EyebrowLabel(
                    when {
                        ready != null -> "Video found"
                        failed != null -> "Not found"
                        else -> "Looking up"
                    },
                )
                Box(Modifier.size(YtDimens.MinTouch))
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                YtCard(Modifier.fillMaxWidth().height(192.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(YtTheme.colors.thumbnail),
                        contentAlignment = Alignment.Center,
                    ) {
                        val bitmap = ready?.meta?.thumbnail
                        val thumbAlpha by animateFloatAsState(
                            targetValue = if (bitmap != null) 1f else 0f,
                            animationSpec = motionTween(),
                            label = "thumbnailFade",
                        )
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer { alpha = thumbAlpha },
                                contentScale = ContentScale.Crop,
                            )
                        }
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = YtTheme.colors.track,
                                strokeWidth = 3.dp,
                            )
                        }
                        if (ready != null) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(MaterialTheme.colorScheme.secondary, PillShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                YtIcon(
                                    R.drawable.ic_play,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    size = 22.dp,
                                )
                            }
                            Text(
                                formatDuration(ready.meta.durationSeconds),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(MaterialTheme.colorScheme.secondary, PillShape)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondary,
                            )
                        }
                    }
                }
                Column(
                    modifier = Modifier.animateContentSize(motionSpring()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when {
                        ready != null -> {
                            Text(
                                ready.meta.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (ready.meta.uploader.isNotBlank()) {
                                Text(
                                    ready.meta.uploader,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        failed != null -> {
                            Text(
                                "Could not load this video",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                failed.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        else -> {
                            Text(
                                "Fetching video info",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                "The first lookup can take a few seconds.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = ready != null, enter = ytEnter(), exit = ytExit()) {
              Column(verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap)) {
                if (ready != null) {
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
                    }
                }

                YtCard(Modifier.fillMaxWidth()) {
                    InfoRow("Saves to", if (isVideo) "Movies / $saveFolder" else "Music / $saveFolder")
                    HorizontalDivider(thickness = YtDimens.Hairline, color = MaterialTheme.colorScheme.outline)
                    InfoRow("Length", formatDuration(ready.meta.durationSeconds))
                }
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
                GoldButton(text = "Try again", onClick = onRetry, modifier = Modifier.fillMaxWidth())
            } else {
                GoldButton(
                    text = "Download $formatName",
                    onClick = onDownload,
                    enabled = ready != null,
                    icon = R.drawable.ic_download,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
