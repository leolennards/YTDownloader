package com.leolennards.ytdownloader.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.QueueSummary
import com.leolennards.ytdownloader.data.extractLinks
import com.leolennards.ytdownloader.data.isPlaylistLink
import com.leolennards.ytdownloader.ui.components.DownloadRow
import com.leolennards.ytdownloader.ui.components.GoldButton
import com.leolennards.ytdownloader.ui.components.IconTile
import com.leolennards.ytdownloader.ui.components.OutlinePillButton
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SegmentOption
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.components.YtProgressBar
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

val FormatOptions = listOf(
    SegmentOption("MP3 audio", R.drawable.ic_music),
    SegmentOption("MP4 video", R.drawable.ic_video),
)

@Composable
fun PasteLinkScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    formatIndex: Int,
    onFormatChange: (Int) -> Unit,
    onFetch: () -> Unit,
    onQueue: (List<String>) -> Unit,
    onSeeAll: () -> Unit,
    queue: QueueSummary?,
    recent: DownloadItem?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val links = remember(url) { extractLinks(url) }
    val playlistCount = links.count { isPlaylistLink(it) }
    val hint = when {
        links.size == 1 && playlistCount == 1 -> "Playlist found. Every video in it will be added to the queue."
        links.size > 1 && playlistCount > 0 -> "${links.size} links found, including $playlistCount playlist"
        links.size > 1 -> "${links.size} links found"
        else -> null
    }
    // keep the old hint so it can fade out
    val lastHint = remember { arrayOf("") }
    if (hint != null) lastHint[0] = hint

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = YtDimens.ScreenPadding)
            .padding(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
    ) {
        ScreenHeader(
            eyebrow = "New download",
            title = "Paste a link",
            subtitle = "Save videos as MP3 or MP4. Paste one link or a whole list.",
            modifier = Modifier.staggeredEntrance(0, "paste-0"),
        )

        YtCard(Modifier.fillMaxWidth().staggeredEntrance(1, "paste-1")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Video link",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    YtIcon(R.drawable.ic_link, tint = YtTheme.colors.goldText)
                    BasicTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 4,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        decorationBox = { innerTextField ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (url.isEmpty()) {
                                    Text(
                                        "https://",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Button(
                        onClick = {
                            haptics.tick()
                            readClipboard(context)?.let(onUrlChange)
                        },
                        modifier = Modifier.height(YtDimens.MinTouch),
                        shape = PillShape,
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = YtTheme.colors.tonal,
                            contentColor = YtTheme.colors.goldText,
                        ),
                    ) {
                        Text("Paste", style = MaterialTheme.typography.labelLarge)
                    }
                }
                AnimatedVisibility(visible = hint != null, enter = ytEnter(), exit = ytExit()) {
                    Text(
                        lastHint[0],
                        style = MaterialTheme.typography.labelMedium,
                        color = YtTheme.colors.goldText,
                    )
                }
            }
        }

        Column(
            modifier = Modifier.staggeredEntrance(2, "paste-2"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Format",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(FormatOptions, formatIndex, onFormatChange)
        }

        Column(
            modifier = Modifier.staggeredEntrance(3, "paste-3"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GoldButton(
                text = "Fetch video",
                onClick = onFetch,
                enabled = links.size == 1 && playlistCount == 0,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinePillButton(
                text = when {
                    links.size == 1 && playlistCount == 1 -> "Add playlist to queue"
                    links.size > 1 -> "Add ${links.size} to queue"
                    else -> "Add to queue"
                },
                onClick = { onQueue(links) },
                enabled = links.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                if (formatIndex == 1) {
                    "Queue saves video at 720p. Fetch video lets you choose the quality."
                } else {
                    "Only save videos you own or have permission to download."
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = queue != null, enter = ytEnter(), exit = ytExit()) {
            // same for the queue summary
            val shown = remember { arrayOfNulls<QueueSummary>(1) }
            if (queue != null) shown[0] = queue
            shown[0]?.let { summary ->
                YtCard(Modifier.fillMaxWidth().ytClickable(onClick = onSeeAll)) {
                    Column(
                        modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            IconTile(R.drawable.ic_download)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Downloading ${summary.running}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (summary.waiting > 0) "${summary.waiting} waiting in the queue" else "Tap to see progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        YtProgressBar(summary.progress, Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        Column(
            modifier = Modifier.staggeredEntrance(4, "paste-4"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Box(
                    modifier = Modifier
                        .height(YtDimens.MinTouch)
                        .ytClickable(onClick = onSeeAll),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "See all",
                        style = MaterialTheme.typography.labelLarge,
                        color = YtTheme.colors.goldText,
                    )
                }
            }
            if (recent != null) {
                DownloadRow(recent)
            } else {
                Text(
                    "Downloads you save will show up here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun readClipboard(context: Context): String? {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
