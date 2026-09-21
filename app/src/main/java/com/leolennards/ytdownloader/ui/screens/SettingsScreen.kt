package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.data.SettingsState
import com.leolennards.ytdownloader.ui.components.ChoicePill
import com.leolennards.ytdownloader.ui.components.OutlinePillButton
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

// small grey title above an option
@Composable
private fun OptionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    // the folder name is kept here while typing, so the cursor does not jump
    var folder by remember { mutableStateOf(settings.folderName) }
    val folderShown = folder.trim().ifBlank { "Downloader" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = YtDimens.ScreenPadding)
            .padding(top = 24.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
    ) {
        ScreenHeader(
            eyebrow = "Preferences",
            title = "Settings",
            subtitle = "Choose how the app behaves.",
            modifier = Modifier.staggeredEntrance(0, "settings-0"),
        )

        // ---- defaults for new downloads ----
        YtCard(Modifier.fillMaxWidth().staggeredEntrance(1, "settings-1")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "New downloads",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OptionLabel("Default format")
                SegmentedControl(
                    options = FormatOptions,
                    selectedIndex = settings.defaultFormatIndex,
                    onSelect = { onChange(settings.copy(defaultFormatIndex = it)) },
                )
                AnimatedVisibility(visible = settings.defaultFormatIndex == 1, enter = ytEnter(), exit = ytExit()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionLabel("Default video quality")
                        QualityPills(settings.defaultHeight) { onChange(settings.copy(defaultHeight = it)) }
                    }
                }
                Text(
                    "Used when you add links from the paste screen. You can still change the format there.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- shared links ----
        YtCard(Modifier.fillMaxWidth().staggeredEntrance(2, "settings-2")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Download shared links",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "When you share a link to YTDownloader from YouTube, it downloads straight away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = settings.autoDownloadShares,
                        onCheckedChange = {
                            haptics.tick()
                            onChange(settings.copy(autoDownloadShares = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = YtTheme.colors.track,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }

                AnimatedVisibility(visible = settings.autoDownloadShares, enter = ytEnter(), exit = ytExit()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OptionLabel("Format for shared links")
                        SegmentedControl(
                            options = FormatOptions,
                            selectedIndex = settings.shareFormatIndex,
                            onSelect = { onChange(settings.copy(shareFormatIndex = it)) },
                        )
                        AnimatedVisibility(visible = settings.shareFormatIndex == 1, enter = ytEnter(), exit = ytExit()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OptionLabel("Video quality")
                                QualityPills(settings.shareHeight) { onChange(settings.copy(shareHeight = it)) }
                            }
                        }
                    }
                }
            }
        }

        // ---- where files go and how fast ----
        YtCard(Modifier.fillMaxWidth().staggeredEntrance(3, "settings-3")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Saving",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OptionLabel("Folder name")
                Box(
                    Modifier
                        .fillMaxWidth()
                        .border(YtDimens.Hairline, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
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
                Text(
                    "Music goes in Music / $folderShown and videos in Movies / $folderShown. Files you already saved stay where they are.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OptionLabel("Downloads at the same time")
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
                Text(
                    "More is faster, but YouTube can block some downloads when there are too many at once.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- the yt-dlp engine ----
        YtCard(Modifier.fillMaxWidth().staggeredEntrance(4, "settings-4")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Download engine",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "yt-dlp version: ${engineVersion ?: "not known yet"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "The app updates it when it starts. If downloads stop working, update it by hand here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinePillButton(
                    text = if (updating) "Updating..." else "Update now",
                    onClick = onUpdate,
                    enabled = !updating,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (updateMessage != null) {
                    Text(
                        updateMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = YtTheme.colors.goldText,
                    )
                }
            }
        }

        Text(
            "Only save videos you own or have permission to download.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.staggeredEntrance(5, "settings-5"),
        )
    }
}
