package com.leolennards.ytdownloader.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.Color
import com.leolennards.ytdownloader.data.formatSize
import com.leolennards.ytdownloader.ui.components.Artwork
import com.leolennards.ytdownloader.ui.components.Equalizer
import com.leolennards.ytdownloader.ui.components.ItemMenu
import com.leolennards.ytdownloader.ui.components.sharedArt
import com.leolennards.ytdownloader.ui.theme.ytClickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueJob
import com.leolennards.ytdownloader.ui.components.DownloadRow
import com.leolennards.ytdownloader.ui.components.EmptyState
import com.leolennards.ytdownloader.ui.components.QueueJobCard
import com.leolennards.ytdownloader.ui.components.RoundIconButton
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SectionHeader
import com.leolennards.ytdownloader.ui.components.SegmentOption
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.LocalBottomClearance
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance

private val FilterOptions = listOf(SegmentOption("All"), SegmentOption("Music"), SegmentOption("Videos"))

@Composable
fun LibraryScreen(
    items: List<DownloadItem>,
    jobs: List<QueueJob>,
    filterIndex: Int,
    onFilterChange: (Int) -> Unit,
    onCancelJob: (String) -> Unit,
    onRetryJob: (String) -> Unit,
    onDismissJob: (String) -> Unit,
    onCancelAll: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onPlay: (DownloadItem, List<DownloadItem>) -> Unit,
    onDelete: (DownloadItem) -> Unit,
    onAddFirst: () -> Unit,
    modifier: Modifier = Modifier,
    playingUri: String? = null,
    isPlaying: Boolean = false,
    grid: Boolean = false,
    onGridChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val filter = when (filterIndex) {
        1 -> MediaFormat.MP3
        2 -> MediaFormat.MP4
        else -> null
    }
    val filtered = items.filter {
        (filter == null || it.format == filter) && (it.title + " " + it.artist.orEmpty()).contains(query.trim(), ignoreCase = true)
    }
    // done ones show in the saved list below, so only running or failed ones here
    val visibleJobs = jobs.filter { it.status != JobStatus.Done && (filter == null || it.format == filter) }
    val activeCount = visibleJobs.count { it.isActive }

    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    val songs = items.count { it.format == MediaFormat.MP3 }
    val videos = items.size - songs

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = YtDimens.ScreenPadding,
            end = YtDimens.ScreenPadding,
            top = 24.dp,
            bottom = LocalBottomClearance.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header", span = full) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.staggeredEntrance(0, "library-0")) {
                ScreenHeader(
                    eyebrow = "Your downloads",
                    title = "Library",
                    trailing = {
                        // switches between the list and big covers, the icon shows what you will get
                        RoundIconButton(
                            if (grid) R.drawable.ic_list else R.drawable.ic_grid,
                            if (grid) "Show as a list" else "Show as a grid",
                            onClick = { onGridChange(!grid) },
                            tint = MaterialTheme.colorScheme.onSurface,
                            background = YtTheme.colors.surfaceHigh,
                        )
                    },
                )
                if (items.isNotEmpty()) {
                    // the whole collection at a glance
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill(R.drawable.ic_music, if (songs == 1) "1 song" else "$songs songs")
                        if (videos > 0) StatPill(R.drawable.ic_video, if (videos == 1) "1 video" else "$videos videos")
                        StatPill(R.drawable.ic_folder, formatSize(items.sumOf { it.sizeBytes }))
                    }
                }
            }
        }
        item(key = "controls", span = full) {
            Column(
                modifier = Modifier
                    .padding(top = YtDimens.SectionGap, bottom = 8.dp)
                    .staggeredEntrance(1, "library-1"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SearchField(query, onQueryChange)
                SegmentedControl(FilterOptions, filterIndex, onFilterChange)
            }
        }

        if (visibleJobs.isNotEmpty()) {
            item(key = "queue-header", span = full) {
                SectionHeader(
                    if (activeCount > 0) "Downloading · $activeCount" else "Needs attention",
                    modifier = Modifier.animateItem(),
                    actionLabel = if (activeCount >= 2) "Cancel all" else null,
                    onAction = onCancelAll,
                )
            }
            items(visibleJobs, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { job ->
                QueueJobCard(
                    job = job,
                    onCancel = { onCancelJob(job.id) },
                    onRetry = { onRetryJob(job.id) },
                    onDismiss = { onDismissJob(job.id) },
                    modifier = Modifier
                        .animateItem()
                        .padding(bottom = 10.dp),
                )
            }
        }

        if (filtered.isNotEmpty()) {
            item(key = "saved-header", span = full) {
                SectionHeader(
                    when {
                        query.isNotBlank() -> "Results"
                        visibleJobs.isNotEmpty() -> "Saved"
                        else -> "Recently added"
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        items(
            filtered,
            key = { it.uri ?: it.title + it.meta },
            span = { if (grid) GridItemSpan(1) else GridItemSpan(maxLineSpan) },
        ) { item ->
            val uri = item.uri
            val nowPlaying = if (uri != null && uri == playingUri) isPlaying else null
            val play = uri?.let { { onPlay(item, filtered.filter { it.uri != null }) } }
            val shareIt = uri?.let { { share(context, item, it) } }
            val deleteIt = uri?.let { { onDelete(item) } }
            when {
                grid -> CoverTile(item, nowPlaying, play, shareIt, deleteIt, Modifier.animateItem().padding(bottom = 18.dp))
                uri == null -> Box(Modifier.animateItem()) { DownloadRow(item) }
                else -> SwipeToDelete(onDelete = { onDelete(item) }, modifier = Modifier.animateItem()) {
                    DownloadRow(item = item, nowPlaying = nowPlaying, onClick = play, onShare = shareIt, onDelete = deleteIt)
                }
            }
        }

        if (filtered.isEmpty() && visibleJobs.isEmpty()) {
            item(key = "empty", span = full) {
                when {
                    query.isNotBlank() -> EmptyState(
                        R.drawable.ic_search,
                        "No matches",
                        "Nothing in your library matches \"${query.trim()}\".",
                        actionLabel = "Clear search",
                        onAction = { onQueryChange("") },
                    )
                    items.isEmpty() -> EmptyState(
                        R.drawable.ic_download,
                        "Nothing saved yet",
                        "Paste a link and your music and videos will show up here.",
                        actionLabel = "Grab something",
                        onAction = onAddFirst,
                    )
                    else -> EmptyState(
                        if (filter == MediaFormat.MP4) R.drawable.ic_video else R.drawable.ic_music,
                        if (filter == MediaFormat.MP4) "No videos yet" else "No music yet",
                        "Switch the filter to see everything else you saved.",
                        actionLabel = "Show all",
                        onAction = { onFilterChange(0) },
                    )
                }
            }
        }
    }
}

// a small rounded label with an icon, for the numbers under the library title
@Composable
private fun StatPill(icon: Int, text: String) {
    Row(
        modifier = Modifier
            .background(YtTheme.colors.surfaceHigh, PillShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        YtIcon(icon, tint = YtTheme.colors.accentText, size = 15.dp)
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

// a big square cover with the title under it, for the grid view
@Composable
private fun CoverTile(
    item: DownloadItem,
    nowPlaying: Boolean?,
    onClick: (() -> Unit)?,
    onShare: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.then(if (onClick != null) Modifier.ytClickable(pressedScale = 0.96f, onClick = onClick) else Modifier),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Artwork(
                item.uri,
                item.format,
                Modifier.fillMaxSize().sharedArt(item.uri),
                shape = MaterialTheme.shapes.medium,
                loadSize = 320.dp,
                iconSize = 32.dp,
            )
            if (nowPlaying != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Equalizer(nowPlaying, color = Color.White, height = 12.dp)
                }
            }
            if (item.format == MediaFormat.MP4) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.6f), PillShape)
                        .padding(6.dp),
                ) {
                    YtIcon(R.drawable.ic_video, tint = Color.White, size = 14.dp)
                }
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (nowPlaying != null) YtTheme.colors.accentText else MaterialTheme.colorScheme.onBackground,
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
            if (onShare != null || onDelete != null) ItemMenu(onShare, onDelete)
        }
    }
}

// swipe a row to the left to delete it. the menu has delete too, for people who do not swipe
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val haptics = rememberHaptics()
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                haptics.confirm()
                onDelete()
            }
            false
        },
        positionalThreshold = { distance -> distance * 0.4f },
    )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val past = state.targetValue == SwipeToDismissBoxValue.EndToStart
            val bg by animateColorAsState(
                if (past) MaterialTheme.colorScheme.error else YtTheme.colors.surfaceHigh,
                motionTween(),
                label = "swipeBg",
            )
            val iconScale by animateFloatAsState(if (past) 1.2f else 0.9f, motionSpring(dampingRatio = 0.5f), label = "swipeIcon")
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(bg)
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                YtIcon(
                    R.drawable.ic_delete,
                    tint = if (past) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 22.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                )
            }
        },
    ) {
        Box(Modifier.background(MaterialTheme.colorScheme.background)) { content() }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(YtDimens.MinTouch + 4.dp)
            .background(YtTheme.colors.surfaceHigh, PillShape)
            .padding(start = 18.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        YtIcon(R.drawable.ic_search, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search songs, videos, artists",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { onQueryChange(it.take(60)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            RoundIconButton(R.drawable.ic_close, "Clear search", { onQueryChange("") })
        }
    }
}

private fun share(context: Context, item: DownloadItem, uri: String) {
    val mime = if (item.format == MediaFormat.MP3) "audio/mpeg" else "video/mp4"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share"))
}
