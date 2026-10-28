package com.leolennards.ytdownloader.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.CompositionLocalProvider
import com.leolennards.ytdownloader.data.Playback
import com.leolennards.ytdownloader.ui.components.LocalScreenScope
import com.leolennards.ytdownloader.ui.components.LocalSharedScope
import com.leolennards.ytdownloader.ui.components.MiniPlayer
import com.leolennards.ytdownloader.ui.screens.MusicPlayerScreen
import com.leolennards.ytdownloader.ui.screens.VideoPlayerScreen
import com.leolennards.ytdownloader.ui.theme.LocalBottomClearance
import com.leolennards.ytdownloader.ui.theme.YtDimens
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.leolennards.ytdownloader.MainActivity
import com.leolennards.ytdownloader.data.AppSettings
import com.leolennards.ytdownloader.data.DownloadController
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.FetchState
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueSummary
import com.leolennards.ytdownloader.data.Shazam
import com.leolennards.ytdownloader.data.extractLinks
import com.leolennards.ytdownloader.data.isPlaylistLink
import com.leolennards.ytdownloader.data.toItem
import com.leolennards.ytdownloader.ui.components.ArtworkCache
import com.leolennards.ytdownloader.ui.components.Tab
import com.leolennards.ytdownloader.ui.components.ToastHost
import com.leolennards.ytdownloader.ui.components.YtBottomBar
import com.leolennards.ytdownloader.ui.screens.DownloadingScreen
import com.leolennards.ytdownloader.ui.screens.LibraryScreen
import com.leolennards.ytdownloader.ui.screens.PasteLinkScreen
import com.leolennards.ytdownloader.ui.screens.PreviewScreen
import com.leolennards.ytdownloader.ui.screens.QualityHeights
import com.leolennards.ytdownloader.ui.screens.SettingsScreen
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.Motion
import com.leolennards.ytdownloader.ui.theme.YTDownloaderTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.ytSpring
import com.leolennards.ytdownloader.ui.theme.ytTween
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// depth 0 = main tabs, bigger = further in. used for the slide direction
enum class Screen(val depth: Int) {
    Paste(0),
    Library(0),
    Preview(1),
    Downloading(2),
    Settings(0),
    Player(2),
}


// simple navigation with state, no navigation library needed
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun YtApp(
    sharedText: String? = null,
    onSharedHandled: () -> Unit = {},
    // which screen a home screen shortcut asked for
    shortcut: String? = null,
    onShortcutHandled: () -> Unit = {},
    // closes the app screen, used to go back to the app a link was shared from
    onFinish: () -> Unit = {},
    hasCookies: Boolean = false,
    onPickCookies: () -> Unit = {},
    onClearCookies: () -> Unit = {},
) {
    var screen by rememberSaveable { mutableStateOf(Screen.Paste) }
    var url by rememberSaveable { mutableStateOf("") }
    // start with the defaults from the settings. 0 = mp3, 1 = mp4
    var formatIndex by rememberSaveable { mutableIntStateOf(AppSettings.state.value.defaultFormatIndex) }
    var qualityIndex by rememberSaveable {
        mutableIntStateOf(QualityHeights.indexOf(AppSettings.state.value.defaultHeight).coerceAtLeast(0))
    }
    var libraryFilter by rememberSaveable { mutableIntStateOf(0) }
    var libraryQuery by rememberSaveable { mutableStateOf("") }
    // videos play in the player screen itself, music plays in Playback so it keeps going
    var videoList by remember { mutableStateOf<List<DownloadItem>>(emptyList()) }
    var videoIndex by rememberSaveable { mutableIntStateOf(0) }
    var playerIsVideo by rememberSaveable { mutableStateOf(false) }
    // the player shows lyrics instead of the cover, kept while you move around the app
    var lyricsOpen by rememberSaveable { mutableStateOf(false) }
    // files waiting a moment before they are really deleted, so undo is possible
    var pendingDeletes by remember { mutableStateOf<Set<String>>(emptySet()) }
    // where to go back to from the player
    var playerReturn by rememberSaveable { mutableStateOf(Screen.Library) }
    var focusJobId by rememberSaveable { mutableStateOf<String?>(null) }

    val fetch by DownloadController.fetchState.collectAsState()
    val queue by DownloadController.queue.collectAsState()
    val history by DownloadController.history.collectAsState()
    val settings by AppSettings.state.collectAsState()
    val engineVersion by DownloadController.engineVersion.collectAsState()
    val updatingEngine by DownloadController.updating.collectAsState()
    val updateMessage by DownloadController.updateMessage.collectAsState()
    val playback by Playback.state.collectAsState()
    val coverUpgrade by DownloadController.coverUpgrade.collectAsState()
    val coverRevision by DownloadController.coverRevision.collectAsState()
    // covers in saved files changed, so drop the old pictures from memory
    LaunchedEffect(coverRevision) { if (coverRevision > 0) ArtworkCache.clearAll() }
    val items = history.map { it.toItem() }.filter { it.uri == null || it.uri !in pendingDeletes }

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

    // small message at the bottom, with an optional button like undo or play
    var toastText by remember { mutableStateOf("") }
    var toastVisible by remember { mutableStateOf(false) }
    var toastTick by remember { mutableIntStateOf(0) }
    var toastAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var toastLong by remember { mutableStateOf(false) }
    fun showToast(text: String, action: Pair<String, () -> Unit>? = null, long: Boolean = false) {
        toastText = text
        toastAction = action
        toastLong = long
        toastVisible = true
        toastTick++
    }
    // messages from the controller, like when a playlist is read
    LaunchedEffect(Unit) {
        DownloadController.notices.collect { showToast(it, action = "View" to { toastVisible = false; screen = Screen.Library }) }
    }
    LaunchedEffect(toastTick) {
        if (toastTick > 0) {
            delay(if (toastLong) 4500.milliseconds else 2800.milliseconds)
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

    // adds links to the queue (playlists get read first), returns the message to show
    fun queueLinks(links: List<String>, mediaFormat: MediaFormat, height: Int): String {
        val playlists = links.filter { isPlaylistLink(it) }
        val singles = links - playlists.toSet()
        if (singles.isNotEmpty()) {
            DownloadController.enqueueMany(singles.map { it to null }, mediaFormat, height)
        }
        playlists.forEach { DownloadController.enqueuePlaylist(it, mediaFormat, height) }
        return when {
            playlists.isNotEmpty() -> "Reading playlist"
            singles.size == 1 -> "Added to queue"
            else -> "${singles.size} added to queue"
        }
    }

    // a link shared from another app. with auto download on it goes straight to the queue,
    // otherwise it goes in the paste box and you pick the format
    LaunchedEffect(sharedText) {
        if (sharedText != null) {
            val links = extractLinks(sharedText)
            // shazam puts the song's name in the shared text, keep it for when the link is read
            Shazam.remember(sharedText)
            if (links.isNotEmpty()) {
                if (settings.autoDownloadShares) {
                    val sharedFormat = if (settings.shareFormatIndex == 1) MediaFormat.MP4 else MediaFormat.MP3
                    val height = settings.shareHeight
                    val hasPlaylist = links.any { isPlaylistLink(it) }
                    withNotificationPermission {
                        val message = queueLinks(links, sharedFormat, height)
                        haptics.confirm()
                        if (hasPlaylist) {
                            // reading a playlist takes a moment, so stay here and show the queue
                            screen = Screen.Library
                            showToast(message)
                        } else {
                            // downloads keep going in the background, so go back to the app you came from
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            onFinish()
                        }
                    }
                } else {
                    url = links.joinToString("\n")
                    screen = Screen.Paste
                    showToast(if (links.size == 1) "Link added" else "${links.size} links added")
                }
            }
            onSharedHandled()
        }
    }

    LaunchedEffect(shortcut) {
        when (shortcut) {
            MainActivity.ACTION_NEW_DOWNLOAD -> screen = Screen.Paste
            MainActivity.ACTION_OPEN_LIBRARY -> screen = Screen.Library
        }
        if (shortcut != null) onShortcutHandled()
    }

    fun play(item: DownloadItem, list: List<DownloadItem>) {
        val playable = list.filter { it.uri != null && it.format == item.format }.ifEmpty { listOf(item) }
        val index = playable.indexOfFirst { it.uri == item.uri }.coerceAtLeast(0)
        if (item.format == MediaFormat.MP4) {
            // music and a video at the same time would be a mess
            if (playback.playing) Playback.toggle()
            videoList = playable
            videoIndex = index
            playerIsVideo = true
        } else {
            Playback.play(context, playable, index)
            playerIsVideo = false
        }
        playerReturn = if (screen == Screen.Player) playerReturn else screen
        screen = Screen.Player
    }
    val scope = rememberCoroutineScope()
    // hides the file right away and deletes it a few seconds later, unless undo is tapped
    fun delete(item: DownloadItem) {
        val uri = item.uri ?: return
        pendingDeletes = pendingDeletes + uri
        Playback.remove(uri)
        haptics.confirm()
        val job = scope.launch {
            delay(4600.milliseconds)
            withContext(Dispatchers.IO) { DownloadController.deleteSaved(uri) }
            ArtworkCache.forget(uri)
            pendingDeletes = pendingDeletes - uri
        }
        showToast(
            "Deleted \"${item.title.take(24)}\"",
            action = "Undo" to {
                job.cancel()
                pendingDeletes = pendingDeletes - uri
                toastVisible = false
            },
            long = true,
        )
    }

    // when a download finishes and you are not watching it, say so, with a button to play it
    val seenDone = remember { mutableSetOf<String>().apply { addAll(queue.filter { it.status == JobStatus.Done }.map { it.id }) } }
    LaunchedEffect(queue) {
        val newlyDone = queue.filter { it.status == JobStatus.Done && it.id !in seenDone }
        newlyDone.forEach { seenDone.add(it.id) }
        val last = newlyDone.lastOrNull() ?: return@LaunchedEffect
        val watching = screen == Screen.Downloading && last.id == focusJobId
        val result = last.result
        if (!watching && result != null) {
            haptics.confirm()
            showToast(
                if (newlyDone.size > 1) "${newlyDone.size} downloads saved" else "Saved \"${last.title.take(22)}\"",
                action = "Play" to {
                    toastVisible = false
                    play(result, items)
                },
                long = true,
            )
        }
    }

    BackHandler(enabled = screen != Screen.Paste) {
        screen = when (screen) {
            Screen.Player -> playerReturn
            else -> Screen.Paste
        }
    }

    val motion = LocalMotionEnabled.current
    val slidePx = with(LocalDensity.current) { Motion.ScreenSlide.roundToPx() }

    val showBar = screen == Screen.Paste || screen == Screen.Library || screen == Screen.Settings
    val showMini = showBar && playback.current != null
    // room the floating bars take at the bottom, the screens pad their lists by this
    val bottomClearance = YtDimens.NavBarClearance + if (showMini) 76.dp else 0.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
          SharedTransitionLayout {
          CompositionLocalProvider(LocalSharedScope provides this, LocalBottomClearance provides bottomClearance) {
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
              CompositionLocalProvider(LocalScreenScope provides this) {
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
                            Shazam.remember(url)
                            withNotificationPermission {
                                val message = queueLinks(links, format, settings.defaultHeight)
                                url = ""
                                haptics.confirm()
                                showToast(message, action = "View" to { toastVisible = false; screen = Screen.Library })
                            }
                        },
                        onSeeAll = { screen = Screen.Library },
                        onPlay = { play(it, items) },
                        onNothingToPaste = { showToast("Copy a link first, then tap paste") },
                        queue = summary,
                        recent = items.take(10),
                    )

                    Screen.Preview -> PreviewScreen(
                        fetch = fetch,
                        formatIndex = formatIndex,
                        onFormatChange = { formatIndex = it },
                        qualityIndex = qualityIndex,
                        onQualityChange = { qualityIndex = it },
                        onBack = { screen = Screen.Paste },
                        onRetry = { DownloadController.fetch(url.trim()) },
                        saveFolder = settings.folderName.trim().ifBlank { "Downloader" },
                        onDownload = {
                            (fetch as? FetchState.Ready)?.let { ready ->
                                withNotificationPermission {
                                    focusJobId = DownloadController.enqueue(
                                        ready.url,
                                        format,
                                        QualityHeights[qualityIndex],
                                        knownTitle = ready.meta.title,
                                        knownArtist = ready.meta.songArtist,
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
                        onPlay = { play(it, items) },
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
                        query = libraryQuery,
                        onQueryChange = { libraryQuery = it },
                        onPlay = { item, list -> play(item, list) },
                        onDelete = { delete(it) },
                        onAddFirst = { screen = Screen.Paste },
                        playingUri = playback.current?.uri,
                        isPlaying = playback.playing,
                        grid = settings.libraryGrid,
                        onGridChange = { on -> AppSettings.update { it.copy(libraryGrid = on) } },
                    )

                    Screen.Player -> if (playerIsVideo) {
                        VideoPlayerScreen(
                            items = videoList,
                            index = videoIndex,
                            onIndexChange = { videoIndex = it },
                            onBack = { screen = playerReturn },
                        )
                    } else {
                        MusicPlayerScreen(
                            state = playback,
                            onBack = { screen = playerReturn },
                            onToggle = { Playback.toggle() },
                            onSeek = { Playback.seekTo(it) },
                            onPrevious = { Playback.previous() },
                            onNext = { Playback.next() },
                            lyricsOpen = lyricsOpen,
                            onToggleLyrics = { lyricsOpen = !lyricsOpen },
                            lyricsOnline = settings.lyrics,
                        )
                    }

                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        onChange = { new -> AppSettings.update { new } },
                        engineVersion = engineVersion,
                        updating = updatingEngine,
                        updateMessage = updateMessage,
                        onUpdate = { DownloadController.updateEngine() },
                        hasCookies = hasCookies,
                        onPickCookies = onPickCookies,
                        onClearCookies = onClearCookies,
                        coverUpgrade = coverUpgrade,
                        onUpgradeCovers = { DownloadController.upgradeCovers() },
                    )
                }
              }
            }
          }
          }

            ToastHost(
                visible = toastVisible,
                text = toastText,
                actionLabel = toastAction?.first,
                onAction = { toastAction?.second?.invoke() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = if (showBar) bottomClearance - 8.dp else 16.dp),
            )
        }
        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(ytTween(motion)) + slideInVertically(ytSpring(motion)) { it },
            exit = fadeOut(ytTween(motion, Motion.Micro)) + slideOutVertically(ytSpring(motion)) { it },
        ) {
          Column {
            AnimatedVisibility(
                visible = showMini,
                enter = fadeIn(ytTween(motion)) + expandVertically(ytSpring(motion)) + slideInVertically(ytSpring(motion)) { it },
                exit = fadeOut(ytTween(motion, Motion.Micro)) + shrinkVertically(ytSpring(motion)),
            ) {
                // keep the last song so it can animate out
                val shown = remember { arrayOf(playback) }
                if (playback.current != null) shown[0] = playback
                MiniPlayer(
                    state = shown[0],
                    onOpen = {
                        playerIsVideo = false
                        playerReturn = screen
                        screen = Screen.Player
                    },
                    onToggle = { Playback.toggle() },
                    onNext = { Playback.next() },
                    onStop = { Playback.stop() },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
                )
            }
            YtBottomBar(
                badge = active.size,
                selected = when (screen) {
                    Screen.Library -> Tab.Library
                    Screen.Settings -> Tab.Settings
                    else -> Tab.Download
                },
                onSelect = { tab ->
                    when (tab) {
                        Tab.Download -> screen = Screen.Paste
                        Tab.Library -> screen = Screen.Library
                        Tab.Settings -> screen = Screen.Settings
                    }
                },
            )
          }
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
