package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.data.SettingsState
import com.leolennards.ytdownloader.ui.components.ChoicePill
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.SegmentedControl
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit

@Composable
fun SettingsScreen(
    settings: SettingsState,
    onChange: (SettingsState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val isVideo = settings.shareFormatIndex == 1

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

        YtCard(Modifier.fillMaxWidth().staggeredEntrance(1, "settings-1")) {
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
                        Text(
                            "Format for shared links",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        SegmentedControl(
                            options = FormatOptions,
                            selectedIndex = settings.shareFormatIndex,
                            onSelect = { onChange(settings.copy(shareFormatIndex = it)) },
                        )

                        AnimatedVisibility(visible = isVideo, enter = ytEnter(), exit = ytExit()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    "Video quality",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    QualityOptions.forEachIndexed { index, label ->
                                        ChoicePill(
                                            text = label,
                                            selected = QualityHeights[index] == settings.shareHeight,
                                            onClick = { onChange(settings.copy(shareHeight = QualityHeights[index])) },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            "Only save videos you own or have permission to download.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.staggeredEntrance(2, "settings-2"),
        )
    }
}
