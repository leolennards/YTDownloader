package com.leolennards.ytdownloader.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = YtDimens.ScreenPadding,
            end = YtDimens.ScreenPadding,
            top = 24.dp,
            bottom = LocalBottomClearance.current,
        ),
    ) {
        item(key = "header") {
            ScreenHeader(
                eyebrow = "Your downloads",
                title = "Library",
                subtitle = when (items.size) {
                    0 -> "Everything you save shows up here."
                    1 -> "1 file saved on this phone."
                    else -> "${items.size} files saved on this phone."
                },
                modifier = Modifier.staggeredEntrance(0, "library-0"),
            )
        }
        item(key = "controls") {
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
            item(key = "queue-header") {
                SectionHeader(
                    if (activeCount > 0) "Downloading · $activeCount" else "Needs attention",
                    modifier = Modifier.animateItem(),
                    actionLabel = if (activeCount >= 2) "Cancel all" else null,
                    onAction = onCancelAll,
                )
            }
            items(visibleJobs, key = { it.id }) { job ->
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
            item(key = "saved-header") {
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
        items(filtered, key = { it.uri ?: it.title + it.meta }) { item ->
            val uri = item.uri
            val row = @Composable {
                DownloadRow(
                    item = item,
                    nowPlaying = if (uri != null && uri == playingUri) isPlaying else null,
                    onClick = uri?.let { { onPlay(item, filtered.filter { it.uri != null }) } },
                    onShare = uri?.let { { share(context, item, it) } },
                    onDelete = uri?.let { { onDelete(item) } },
                )
            }
            if (uri == null) {
                Box(Modifier.animateItem()) { row() }
            } else {
                SwipeToDelete(onDelete = { onDelete(item) }, modifier = Modifier.animateItem()) { row() }
            }
        }

        if (filtered.isEmpty() && visibleJobs.isEmpty()) {
            item(key = "empty") {
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
