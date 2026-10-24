package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.ui.components.YtIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.data.CoverUpgrade
import com.leolennards.ytdownloader.data.SettingsState
import com.leolennards.ytdownloader.ui.components.YtProgressBar
import com.leolennards.ytdownloader.ui.components.ChoicePill
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SecondaryButton
import com.leolennards.ytdownloader.ui.components.SegmentOption
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.theme.LocalBottomClearance
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

// a titled group of settings in one card, rows split by thin lines
@Composable
private fun SettingsGroup(
    title: String,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.staggeredEntrance(index, "settings-$title"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EyebrowLabel(title, Modifier.padding(start = 4.dp))
        YtCard(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun GroupDivider() {
    HorizontalDivider(
        thickness = YtDimens.Hairline,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = YtDimens.CardPadding),
    )
}

// one block inside a group: a title, an optional line under it, and the control
@Composable
private fun SettingBlock(
    title: String,
    description: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(YtDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        content?.invoke(this)
    }
}

// a whole row that flips a switch, so the text is tappable too
@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val haptics = rememberHaptics()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .ytClickable(haptic = false, pressedScale = 0.99f) {
                haptics.tick()
                onChange(!checked)
            }
            .semantics { role = Role.Switch }
            .padding(YtDimens.CardPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptics.tick()
                onChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = YtTheme.colors.track,
                uncheckedBorderColor = YtTheme.colors.track,
            ),
        )
    }
}

// the app's icon, name and versions, at the top of settings
@Composable
private fun AboutCard(engineVersion: String?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }
    YtCard(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(YtTheme.colors.ember[0].copy(alpha = 0.14f), YtTheme.colors.ember[2].copy(alpha = 0.10f))),
                )
                .padding(YtDimens.CardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .shadow(12.dp, MaterialTheme.shapes.small, ambientColor = YtTheme.colors.ember[1], spotColor = YtTheme.colors.ember[1])
                    .background(Color(0xFF141418), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                // the app icon's mark, in its own colors
                Image(painterResource(R.drawable.ic_brand_mark), contentDescription = null, modifier = Modifier.size(30.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("AnyGrab", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Version $version · yt-dlp ${engineVersion ?: "checking"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Everything runs on your phone. No accounts, no servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = YtTheme.colors.accentText,
                )
            }
        }
    }
}

// row of pills for choosing a video quality
@Composable
private fun QualityPills(selectedHeight: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QualityOptions.forEachIndexed { index, label ->
            ChoicePill(
                text = label,
                selected = QualityHeights[index] == selectedHeight,
                onClick = { onSelect(QualityHeights[index]) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onChange: (SettingsState) -> Unit,
    engineVersion: String?,
    updating: Boolean,
    updateMessage: String?,
    onUpdate: () -> Unit,
    hasCookies: Boolean = false,
    onPickCookies: () -> Unit = {},
    onClearCookies: () -> Unit = {},
    coverUpgrade: CoverUpgrade? = null,
    onUpgradeCovers: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // the folder name is kept here while typing, so the cursor does not jump
    var folder by remember { mutableStateOf(settings.folderName) }
    val folderShown = folder.trim().ifBlank { "Downloader" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = YtDimens.ScreenPadding)
            .padding(top = 24.dp, bottom = LocalBottomClearance.current),
        verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
    ) {
        ScreenHeader(
            eyebrow = "Preferences",
            title = "Settings",
            subtitle = "Make AnyGrab work the way you like.",
            modifier = Modifier.staggeredEntrance(0, "settings-0"),
        )

        AboutCard(engineVersion, Modifier.staggeredEntrance(1, "settings-about"))

        SettingsGroup("Appearance", 1) {
            SettingBlock("Theme", "System follows your phone's setting.") {
                SegmentedControl(
                    options = listOf(SegmentOption("System"), SegmentOption("Light"), SegmentOption("Dark")),
                    selectedIndex = settings.themeMode,
                    onSelect = { onChange(settings.copy(themeMode = it)) },
                )
            }
        }

        SettingsGroup("New downloads", 2) {
            SettingBlock("Default format", "What the paste screen starts with. You can still change it there.") {
                SegmentedControl(
                    options = FormatOptions,
                    selectedIndex = settings.defaultFormatIndex,
                    onSelect = { onChange(settings.copy(defaultFormatIndex = it)) },
                )
                AnimatedVisibility(visible = settings.defaultFormatIndex == 1, enter = ytEnter(), exit = ytExit()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Video quality when you skip the preview",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        QualityPills(settings.defaultHeight) { onChange(settings.copy(defaultHeight = it)) }
                    }
                }
            }
            GroupDivider()
            SwitchRow(
                "Download shared links",
                "When you share a link to AnyGrab from another app, it downloads straight away.",
                settings.autoDownloadShares,
            ) { onChange(settings.copy(autoDownloadShares = it)) }
            AnimatedVisibility(visible = settings.autoDownloadShares, enter = ytEnter(), exit = ytExit()) {
                Column(
                    modifier = Modifier.padding(start = YtDimens.CardPadding, end = YtDimens.CardPadding, bottom = YtDimens.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SegmentedControl(
                        options = FormatOptions,
                        selectedIndex = settings.shareFormatIndex,
                        onSelect = { onChange(settings.copy(shareFormatIndex = it)) },
                    )
                    AnimatedVisibility(visible = settings.shareFormatIndex == 1, enter = ytEnter(), exit = ytExit()) {
                        QualityPills(settings.shareHeight) { onChange(settings.copy(shareHeight = it)) }
                    }
                }
            }
        }

        SettingsGroup("Album art and lyrics", 3) {
            SwitchRow(
                "Official album covers",
                "Gets the real cover in high resolution from Apple Music or Deezer, instead of a blurry " +
                    "video thumbnail. Only the song and artist name are sent to look it up. When there is " +
                    "no sure match, the video thumbnail is used.",
                settings.officialCovers,
            ) { onChange(settings.copy(officialCovers = it)) }
            GroupDivider()
            SwitchRow(
                "Lyrics",
                "Finds the words of your songs on LRCLIB, a free lyrics library. The player shows them in time " +
                    "with the music, and they are saved inside the MP3 so other music apps show them too.",
                settings.lyrics,
            ) { onChange(settings.copy(lyrics = it)) }
            AnimatedVisibility(visible = settings.officialCovers || settings.lyrics, enter = ytEnter(), exit = ytExit()) {
                Column {
                    GroupDivider()
                    SettingBlock(
                        "Upgrade the songs you already have",
                        "Adds the official cover and the lyrics to the songs already in your library. The music itself is not changed.",
                    ) {
                        val running = coverUpgrade != null && !coverUpgrade.finished
                        if (coverUpgrade != null) {
                            YtProgressBar(coverUpgrade.done.toFloat() / coverUpgrade.total.coerceAtLeast(1), height = 4.dp)
                            Text(
                                when {
                                    !coverUpgrade.finished -> "Checking ${coverUpgrade.done} of ${coverUpgrade.total} songs"
                                    coverUpgrade.improved == 0 && coverUpgrade.lyrics == 0 -> "Nothing new found this time"
                                    else -> listOfNotNull(
                                        coverUpgrade.improved.takeIf { it > 0 }?.let { if (it == 1) "1 cover sharpened" else "$it covers sharpened" },
                                        coverUpgrade.lyrics.takeIf { it > 0 }?.let { if (it == 1) "lyrics for 1 song" else "lyrics for $it songs" },
                                    ).joinToString(", ").replaceFirstChar { it.uppercase() }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (coverUpgrade.finished && (coverUpgrade.improved > 0 || coverUpgrade.lyrics > 0)) YtTheme.colors.success else YtTheme.colors.accentText,
                            )
                        }
                        SecondaryButton(
                            text = if (running) "Working on it..." else "Upgrade my songs",
                            onClick = onUpgradeCovers,
                            enabled = !running,
                            contentColor = YtTheme.colors.accentText,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        SettingsGroup("Saving", 4) {
            SettingBlock(
                "Folder name",
                "Music goes in Music / $folderShown and videos in Movies / $folderShown. Files you already saved stay where they are.",
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(YtTheme.colors.surfaceHigh, MaterialTheme.shapes.small)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    BasicTextField(
                        value = folder,
                        onValueChange = {
                            folder = it.take(40)
                            onChange(settings.copy(folderName = folder))
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            GroupDivider()
            SettingBlock(
                "Downloads at the same time",
                "More is faster, but some sites can block downloads when there are too many at once.",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { count ->
                        ChoicePill(
                            text = count.toString(),
                            selected = settings.maxParallel == count,
                            onClick = { onChange(settings.copy(maxParallel = count)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            GroupDivider()
            SwitchRow(
                "Wi-Fi only",
                "Downloads wait until you are on Wi-Fi so they do not use your mobile data.",
                settings.wifiOnly,
            ) { onChange(settings.copy(wifiOnly = it)) }
        }

        SettingsGroup("Sites that need a login", 4) {
            SettingBlock(
                "Login cookies",
                "Some sites, like Instagram, need you to be logged in. Export your cookies to a text file " +
                    "from a browser where you are logged in, then add that file here. Cookies stop working " +
                    "after a while, so you may need to add a fresh file later.",
            ) {
                if (hasCookies) {
                    Text("Cookies added", style = MaterialTheme.typography.labelLarge, color = YtTheme.colors.success)
                    SecondaryButton(
                        text = "Remove cookies",
                        onClick = onClearCookies,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SecondaryButton(
                        text = "Choose cookies file",
                        onClick = onPickCookies,
                        contentColor = YtTheme.colors.accentText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SettingsGroup("Download engine", 5) {
            SettingBlock(
                "yt-dlp ${engineVersion ?: "(checking)"}",
                "AnyGrab updates it every time it starts. If downloads stop working, update it by hand here.",
            ) {
                SecondaryButton(
                    text = if (updating) "Updating..." else "Update now",
                    onClick = onUpdate,
                    enabled = !updating,
                    contentColor = YtTheme.colors.accentText,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (updateMessage != null) {
                    Text(updateMessage, style = MaterialTheme.typography.bodySmall, color = YtTheme.colors.accentText)
                }
            }
        }

        Text(
            "Only save content you own or have permission to download.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .staggeredEntrance(6, "settings-6"),
        )
    }
}
