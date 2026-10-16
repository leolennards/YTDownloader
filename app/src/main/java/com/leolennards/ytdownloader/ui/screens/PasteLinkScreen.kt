package com.leolennards.ytdownloader.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.content.ClipDescription
import android.os.Build
import android.view.textclassifier.TextClassifier
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import com.leolennards.ytdownloader.ui.components.sharedArt
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.QueueSummary
import com.leolennards.ytdownloader.data.extractLinks
import com.leolennards.ytdownloader.data.isPlaylistLink
import com.leolennards.ytdownloader.ui.components.Artwork
import com.leolennards.ytdownloader.ui.components.IconTile
import com.leolennards.ytdownloader.ui.components.PrimaryButton
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SecondaryButton
import com.leolennards.ytdownloader.ui.components.SectionHeader
import com.leolennards.ytdownloader.ui.components.SegmentOption
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.TextAction
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.components.YtProgressBar
import com.leolennards.ytdownloader.ui.theme.LocalBottomClearance
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionTween
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
    onPlay: (DownloadItem) -> Unit,
    queue: QueueSummary?,
    recent: List<DownloadItem>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val links = remember(url) { extractLinks(url) }
    val playlistCount = links.count { isPlaylistLink(it) }
    // one normal link opens the preview, anything else goes straight to the queue
    val single = links.size == 1 && playlistCount == 0
    val hint = when {
        links.size == 1 && playlistCount == 1 -> "Playlist found. Every track in it will be added."
        links.size > 1 && playlistCount > 0 -> "${links.size} links found, including $playlistCount playlist"
        links.size > 1 -> "${links.size} links found"
        url.isNotBlank() && links.isEmpty() -> "That does not look like a link yet"
        else -> null
    }
    // a link sitting on the clipboard, offered as a one tap paste
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var copiedLink by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(windowFocused, url) {
        if (windowFocused) copiedLink = clipboardLink(context)?.takeIf { it.trim() != url.trim() }
    }
    // keep the old hint so it can fade out
    val lastHint = remember { arrayOf("") }
    if (hint != null) lastHint[0] = hint
    val fieldBorder by animateColorAsState(
        if (links.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        motionTween(),
        label = "fieldBorder",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 24.dp, bottom = LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
    ) {
        Box {
            AmbientGlow(Modifier.matchParentSize())
            ScreenHeader(
                eyebrow = "AnyGrab",
                title = "Grab anything",
                subtitle = "Paste a link from YouTube, TikTok, Instagram, X, SoundCloud and more.",
                modifier = Modifier
                    .padding(horizontal = YtDimens.ScreenPadding)
                    .staggeredEntrance(0, "paste-0"),
            )
        }

        YtCard(
            Modifier
                .padding(horizontal = YtDimens.ScreenPadding)
                .fillMaxWidth()
                .staggeredEntrance(1, "paste-1"),
            borderColor = fieldBorder,
        ) {
            Column(Modifier.padding(YtDimens.CardPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    YtIcon(
                        R.drawable.ic_link,
                        tint = YtTheme.colors.accentText,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    BasicTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        modifier = Modifier.weight(1f),
                        minLines = 1,
                        maxLines = 5,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        decorationBox = { innerTextField ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (url.isEmpty()) {
                                    Text(
                                        "Paste one link, or a few",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    if (url.isNotEmpty()) {
                        RoundIconButton(R.drawable.ic_close, "Clear", { onUrlChange("") })
                    }
                }
                // the copied link, ready to use. tapping it fills the box
                AnimatedVisibility(visible = copiedLink != null && url.isBlank(), enter = ytEnter(), exit = ytExit()) {
                    val shown = remember { arrayOf("") }
                    copiedLink?.let { shown[0] = it }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .ytClickable(haptic = false, pressedScale = 0.98f) {
                                haptics.confirm()
                                onUrlChange(shown[0])
                            }
                            .background(YtTheme.colors.tonal)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        YtIcon(R.drawable.ic_clipboard, tint = YtTheme.colors.accentText, size = 18.dp)
                        Column(Modifier.weight(1f)) {
                            Text("Use the link you copied", style = MaterialTheme.typography.titleSmall, color = YtTheme.colors.accentText)
                            Text(
                                shown[0].removePrefix("https://").removePrefix("www."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        YtIcon(R.drawable.ic_arrow_right, tint = YtTheme.colors.accentText, size = 18.dp)
                    }
                }
                SecondaryButton(
                    text = "Paste from clipboard",
                    icon = R.drawable.ic_clipboard,
                    onClick = {
                        readClipboard(context)?.let {
                            haptics.confirm()
                            onUrlChange(it)
                        }
                    },
                    contentColor = YtTheme.colors.accentText,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnimatedVisibility(visible = hint != null, enter = ytEnter(), exit = ytExit()) {
                    Text(
                        lastHint[0],
                        style = MaterialTheme.typography.labelMedium,
                        color = if (links.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else YtTheme.colors.accentText,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = YtDimens.ScreenPadding)
                .staggeredEntrance(2, "paste-2"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Save as",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SegmentedControl(FormatOptions, formatIndex, onFormatChange)
        }

        Column(
            modifier = Modifier
                .padding(horizontal = YtDimens.ScreenPadding)
                .staggeredEntrance(3, "paste-3"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PrimaryButton(
                text = when {
                    links.isEmpty() -> "Paste a link to start"
                    single -> "Continue"
                    links.size == 1 -> "Add playlist to queue"
                    else -> "Add ${links.size} to queue"
                },
                icon = if (single) R.drawable.ic_arrow_right else if (links.isNotEmpty()) R.drawable.ic_download else null,
                onClick = { if (single) onFetch() else onQueue(links) },
                enabled = links.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(visible = single, enter = ytEnter(), exit = ytExit()) {
                TextAction("Skip the preview and add to queue", onClick = { onQueue(links) })
            }
            Text(
                "Only save content you own or have permission to download.",
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = queue != null, enter = ytEnter(), exit = ytExit()) {
            // keep the last summary so it can animate out
            val shown = remember { arrayOfNulls<QueueSummary>(1) }
            if (queue != null) shown[0] = queue
            shown[0]?.let { summary ->
                YtCard(
                    Modifier
                        .padding(horizontal = YtDimens.ScreenPadding)
                        .fillMaxWidth()
                        .ytClickable(pressedScale = 0.98f, onClick = onSeeAll),
                ) {
                    Column(
                        modifier = Modifier.padding(YtDimens.CardPadding),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            IconTile(R.drawable.ic_download)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (summary.running == 1) "1 download running" else "${summary.running} downloads running",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    if (summary.waiting > 0) "${summary.waiting} waiting · tap to see the queue" else "Tap to see progress",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            YtIcon(R.drawable.ic_arrow_right, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        YtProgressBar(summary.progress, height = 4.dp)
                    }
                }
            }
        }

        if (recent.isNotEmpty()) {
            Column(modifier = Modifier.staggeredEntrance(4, "paste-4")) {
                SectionHeader(
                    "Recently saved",
                    modifier = Modifier.padding(horizontal = YtDimens.ScreenPadding),
                    actionLabel = "See all",
                    onAction = onSeeAll,
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = YtDimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(recent, key = { it.uri ?: it.title }) { item ->
                        RecentTile(item, onClick = { onPlay(item) })
                    }
                }
            }
        }
    }
}

// square cover with the title under it, for the recent row
@Composable
private fun RecentTile(item: DownloadItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(132.dp)
            .ytClickable(enabled = item.uri != null, pressedScale = 0.96f, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Artwork(
            item.uri,
            item.format,
            Modifier.size(132.dp).sharedArt(item.uri),
            shape = MaterialTheme.shapes.medium,
            loadSize = 264.dp,
            iconSize = 28.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.artist?.takeIf { it.isNotBlank() } ?: item.format.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// a soft amber light that drifts slowly behind the title
@Composable
private fun AmbientGlow(modifier: Modifier) {
    val motion = LocalMotionEnabled.current
    val t by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowT",
    )
    val phase = if (motion) t else 0.5f
    val accent = YtTheme.colors.accent
    Canvas(modifier) {
        val r = size.width * 0.55f
        drawCircle(
            Brush.radialGradient(
                listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * (0.75f + 0.12f * phase), size.height * (0.15f + 0.3f * phase)),
                radius = r,
            ),
            radius = r,
            center = Offset(size.width * (0.75f + 0.12f * phase), size.height * (0.15f + 0.3f * phase)),
        )
    }
}

// a link on the clipboard, without the android 12 "pasted from your clipboard" message.
// newer android can tell from the clip's description if it is a link, older ones can read it quietly
private fun clipboardLink(context: Context): String? {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return null
    val description = manager.primaryClipDescription ?: return null
    if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_URILIST)) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val classified = description.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE
        if (!classified || description.getConfidenceScore(TextClassifier.TYPE_URL) < 0.5f) return null
    }
    return readClipboard(context)?.let { extractLinks(it).firstOrNull() }
}

private fun readClipboard(context: Context): String? {
    val manager = context.getSystemService(ClipboardManager::class.java) ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
}
