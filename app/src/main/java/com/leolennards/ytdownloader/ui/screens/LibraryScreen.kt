package com.leolennards.ytdownloader.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.ui.components.YtIcon
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueJob
import com.leolennards.ytdownloader.ui.components.DownloadRow
import com.leolennards.ytdownloader.ui.components.QueueJobCard
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SegmentOption
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

private val FilterOptions = listOf(SegmentOption("All"), SegmentOption("MP3"), SegmentOption("MP4"))

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
    onPlay: (DownloadItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val filter = when (filterIndex) {
        1 -> MediaFormat.MP3
        2 -> MediaFormat.MP4
        else -> null
    }
    val filtered = items.filter {
        (filter == null || it.format == filter) && it.title.contains(query.trim(), ignoreCase = true)
    }
    // done ones show in the saved list below, so only running or failed ones here
    val visibleJobs = jobs.filter { it.status != JobStatus.Done && (filter == null || it.format == filter) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = YtDimens.ScreenPadding)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
    ) {
        ScreenHeader(
            eyebrow = "Saved",
            title = "Library",
            subtitle = "Everything you have downloaded so far.",
            modifier = Modifier.staggeredEntrance(0, "library-0"),
        )
        SegmentedControl(
            FilterOptions,
            filterIndex,
            onFilterChange,
            modifier = Modifier.staggeredEntrance(1, "library-1"),
        )
        // search box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(YtDimens.Hairline, MaterialTheme.colorScheme.outline, RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .staggeredEntrance(2, "library-2"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            YtIcon(R.drawable.ic_search, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        "Search your downloads",
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
                Box(Modifier.ytClickable { onQueryChange("") }) {
                    YtIcon(R.drawable.ic_close, tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 18.dp, contentDescription = "Clear")
                }
            }
        }
        val activeCount = visibleJobs.count { it.isActive }
        AnimatedVisibility(visible = activeCount >= 2, enter = ytEnter(), exit = ytExit()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$activeCount in the queue",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .height(YtDimens.MinTouch)
                        .ytClickable(onClick = onCancelAll),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Cancel all",
                        style = MaterialTheme.typography.labelLarge,
                        color = YtTheme.colors.goldText,
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(visibleJobs, key = { it.id }) { job ->
                QueueJobCard(
                    job = job,
                    onCancel = { onCancelJob(job.id) },
                    onRetry = { onRetryJob(job.id) },
                    onDismiss = { onDismissJob(job.id) },
                    modifier = Modifier.animateItem(),
                )
            }
            if (filtered.isEmpty() && visibleJobs.isEmpty()) {
                item {
                    Text(
                        if (query.isNotBlank()) "No downloads match that." else "Nothing here yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(filtered, key = { it.uri ?: it.title + it.meta }) { item ->
                DownloadRow(
                    item = item,
                    modifier = Modifier.animateItem(),
                    onClick = item.uri?.let { { onPlay(item) } },
                    onShare = item.uri?.let { uri -> { share(context, item, uri) } },
                )
            }
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
