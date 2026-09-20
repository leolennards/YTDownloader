package com.leolennards.ytdownloader.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.leolennards.ytdownloader.data.DownloadController
import com.leolennards.ytdownloader.data.FetchState
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueSummary
import com.leolennards.ytdownloader.data.isPlaylistLink
import com.leolennards.ytdownloader.data.toItem
import com.leolennards.ytdownloader.ui.components.Tab
import com.leolennards.ytdownloader.ui.components.ToastHost
import com.leolennards.ytdownloader.ui.components.YtBottomBar
import com.leolennards.ytdownloader.ui.screens.DownloadingScreen
import com.leolennards.ytdownloader.ui.screens.LibraryScreen
import com.leolennards.ytdownloader.ui.screens.PasteLinkScreen
import com.leolennards.ytdownloader.ui.screens.PreviewScreen
import com.leolennards.ytdownloader.ui.screens.QualityHeights
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.Motion
import com.leolennards.ytdownloader.ui.theme.YTDownloaderTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.ytSpring
import com.leolennards.ytdownloader.ui.theme.ytTween
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// depth 0 = main tabs, bigger = further in. used for the slide direction
enum class Screen(val depth: Int) {
    Paste(0),
    Library(0),
    Preview(1),
    Downloading(2),
}

// video quality used when adding straight from the paste screen
private const val QUICK_HEIGHT = 720

// simple navigation with state, no navigation library needed
@Composable
fun YtApp() {
    var screen by rememberSaveable { mutableStateOf(Screen.Paste) }
    var url by rememberSaveable { mutableStateOf("") }
    var formatIndex by rememberSaveable { mutableIntStateOf(1) } // 0 = mp3, 1 = mp4
    var qualityIndex by rememberSaveable { mutableIntStateOf(1) }
    var libraryFilter by rememberSaveable { mutableIntStateOf(0) }
    var focusJobId by rememberSaveable { mutableStateOf<String?>(null) }

    val fetch by DownloadController.fetchState.collectAsState()
    val queue by DownloadController.queue.collectAsState()
    val history by DownloadController.history.collectAsState()
    val items = history.map { it.toItem() }

    val active = queue.filter { it.isActive }
    val summary = if (active.isEmpty()) {
        null
    } else {
        QueueSummary(
            running = active.count { it.status == JobStatus.Running },
            waiting = active.count { it.status == JobStatus.Queued },
            progress = active.sumOf { it.progress.toDouble() }.toFloat() / active.size,
        )
    }

    val context = LocalContext.current
    val haptics = rememberHaptics()
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // small message after adding to the queue
    var toastText by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var toastTick by remember { mutableIntStateOf(0) }
    fun showToast(text: String) {
        toastText = text
        toastVisible = true
        toastTick++
    }
    // messages from the controller, like when a playlist is read
    LaunchedEffect(Unit) {
        DownloadController.notices.collect { showToast(it) }
    }
    LaunchedEffect(toastTick) {
        if (toastTick > 0) {
            delay(2800.milliseconds)
            toastVisible = false
        }
    }

    // downloads run either way, the permission just decides if the notification shows
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        pendingAction?.invoke()
        pendingAction = null
    }
    fun withNotificationPermission(action: () -> Unit) {
        val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !askedNotifications &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPrompt) {
            askedNotifications = true
            pendingAction = action
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    val format = if (formatIndex == 1) MediaFormat.MP4 else MediaFormat.MP3

    BackHandler(enabled = screen != Screen.Paste) {
        screen = Screen.Paste
    }

    val motion = LocalMotionEnabled.current
    val slidePx = with(LocalDensity.current) { Motion.ScreenSlide.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = screen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val enterFade = fadeIn(ytTween(motion, Motion.Standard))
                    val exitFade = fadeOut(ytTween(motion, Motion.Micro + 30))
                    when {
                        // switching tabs, just fade
                        targetState.depth == 0 && initialState.depth == 0 -> enterFade togetherWith exitFade
                        // going forward, slide in from the right
                        targetState.depth > initialState.depth -> (enterFade + slideInHorizontally(ytSpring(motion)) { slidePx }) togetherWith
                            (exitFade + slideOutHorizontally(ytSpring(motion)) { -slidePx })
                        // going back, slide the other way
                        else -> (enterFade + slideInHorizontally(ytSpring(motion)) { -slidePx }) togetherWith
                            (exitFade + slideOutHorizontally(ytSpring(motion)) { slidePx })
                    }
                },
                label = "screens",
            ) { target ->
                when (target) {
                    Screen.Paste -> PasteLinkScreen(
                        url = url,
                        onUrlChange = { url = it },
                        formatIndex = formatIndex,
                        onFormatChange = { formatIndex = it },
                        onFetch = {
                            DownloadController.fetch(url.trim())
                            screen = Screen.Preview
                        },
                        onQueue = { links ->
                            withNotificationPermission {
                                val playlists = links.filter { isPlaylistLink(it) }
                                val singles = links - playlists.toSet()
                                if (singles.isNotEmpty()) {
                                    DownloadController.enqueueMany(singles.map { it to null }, format, QUICK_HEIGHT)
                                }
                                playlists.forEach { DownloadController.enqueuePlaylist(it, format, QUICK_HEIGHT) }
                                url = ""
                                haptics.confirm()
                                showToast(
                                    when {
                                        playlists.isNotEmpty() -> "Reading playlist"
                                        singles.size == 1 -> "Added to queue"
                                        else -> "${singles.size} added to queue"
                                    },
                                )
                            }
                        },
                        onSeeAll = { screen = Screen.Library },
                        queue = summary,
                        recent = items.firstOrNull(),
                    )

                    Screen.Preview -> PreviewScreen(
                        fetch = fetch,
                        formatIndex = formatIndex,
                        onFormatChange = { formatIndex = it },
                        qualityIndex = qualityIndex,
                        onQualityChange = { qualityIndex = it },
                        onBack = { screen = Screen.Paste },
                        onRetry = { DownloadController.fetch(url.trim()) },
                        onDownload = {
                            (fetch as? FetchState.Ready)?.let { ready ->
                                withNotificationPermission {
                                    focusJobId = DownloadController.enqueue(
                                        ready.url,
                                        format,
                                        QualityHeights[qualityIndex],
                                        knownTitle = ready.meta.title,
                                    )
                                    url = ""
                                    haptics.confirm()
                                    screen = Screen.Downloading
                                }
                            }
                        },
                    )

                    Screen.Downloading -> DownloadingScreen(
                        job = queue.firstOrNull { it.id == focusJobId },
                        onCancel = {
                            focusJobId?.let { DownloadController.cancel(it) }
                            screen = Screen.Paste
                        },
                        onAddAnother = { screen = Screen.Paste },
                        onViewLibrary = { screen = Screen.Library },
                    )

                    Screen.Library -> LibraryScreen(
                        items = items,
                        jobs = queue,
                        filterIndex = libraryFilter,
                        onFilterChange = { libraryFilter = it },
                        onCancelJob = { DownloadController.cancel(it) },
                        onRetryJob = { DownloadController.retry(it) },
                        onDismissJob = { DownloadController.dismiss(it) },
                        onCancelAll = { DownloadController.cancelAll() },
                    )
                }
            }

            ToastHost(
                visible = toastVisible,
                text = toastText,
                actionLabel = "View",
                onAction = {
                    toastVisible = false
                    screen = Screen.Library
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
        if (screen == Screen.Paste || screen == Screen.Library) {
            YtBottomBar(
                selected = if (screen == Screen.Library) Tab.Library else Tab.Download,
                onSelect = { tab ->
                    when (tab) {
                        Tab.Download -> screen = Screen.Paste
                        Tab.Library -> screen = Screen.Library
                        Tab.Settings -> Unit // no settings screen yet
                    }
                },
            )
        }
    }
}

@Preview(showBackground = true, name = "Light")
@Composable
private fun YtAppLightPreview() {
    YTDownloaderTheme(darkTheme = false) { YtApp() }
}

@Preview(showBackground = true, name = "Dark")
@Composable
private fun YtAppDarkPreview() {
    YTDownloaderTheme(darkTheme = true) { YtApp() }
}
