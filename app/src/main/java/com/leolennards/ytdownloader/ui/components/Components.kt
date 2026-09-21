package com.leolennards.ytdownloader.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.JobStep
import com.leolennards.ytdownloader.data.QueueJob
import com.leolennards.ytdownloader.ui.theme.EyebrowStyle
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.Motion
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.pressScale
import com.leolennards.ytdownloader.ui.theme.rememberAnimatedProgress
import com.leolennards.ytdownloader.ui.theme.rememberHaptics
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytSpring
import com.leolennards.ytdownloader.ui.theme.ytTween

@Composable
fun YtIcon(
    @DrawableRes id: Int,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(id),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}

@Composable
fun YtCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border by animateColorAsState(borderColor, motionTween(), label = "cardBorder")
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(YtDimens.Hairline, border),
    ) {
        Column(content = content)
    }
}

@Composable
fun IconTile(@DrawableRes icon: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(YtTheme.colors.tonal, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        YtIcon(icon, tint = YtTheme.colors.goldText)
    }
}

@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = EyebrowStyle, color = YtTheme.colors.goldText)
}

// screen title, fades when the text changes
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val motion = LocalMotionEnabled.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AnimatedContent(
            targetState = eyebrow,
            transitionSpec = { fadeIn(ytTween(motion)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
            label = "eyebrow",
        ) { text ->
            Text(text.uppercase(), style = EyebrowStyle, color = YtTheme.colors.goldText)
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(ytTween(motion)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
            label = "title",
        ) { text ->
            Text(
                text,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (subtitle != null) {
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { fadeIn(ytTween(motion)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
                label = "subtitle",
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

data class SegmentOption(val label: String, val icon: Int? = null)

// pill toggle, the dark pill slides to the chosen option
@Composable
fun SegmentedControl(
    options: List<SegmentOption>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = 4.dp
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(YtTheme.colors.track, PillShape)
            .padding(4.dp),
    ) {
        val segment = (maxWidth - gap * (options.size - 1)) / options.size
        val indicatorX by animateDpAsState(
            targetValue = (segment + gap) * selectedIndex,
            animationSpec = motionSpring(),
            label = "segmentIndicator",
        )
        Box(
            Modifier
                .offset(x = indicatorX)
                .width(segment)
                .height(YtDimens.MinTouch)
                .background(MaterialTheme.colorScheme.secondary, PillShape),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                val content by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = motionTween(),
                    label = "segmentText",
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(YtDimens.MinTouch)
                        .ytClickable { onSelect(index) }
                        .clip(PillShape),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (option.icon != null) {
                        YtIcon(option.icon, tint = content, size = 18.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(option.label, style = MaterialTheme.typography.titleSmall, color = content)
                }
            }
        }
    }
}

@Composable
fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        motionTween(),
        label = "pillBorder",
    )
    val background by animateColorAsState(
        if (selected) YtTheme.colors.tonal else MaterialTheme.colorScheme.surface,
        motionTween(),
        label = "pillBackground",
    )
    val content by animateColorAsState(
        if (selected) YtTheme.colors.goldText else MaterialTheme.colorScheme.onSurface,
        motionTween(),
        label = "pillText",
    )
    Box(
        modifier = modifier
            .height(YtDimens.MinTouch)
            .ytClickable(onClick = onClick)
            .clip(PillShape)
            .background(background)
            .border(YtDimens.Hairline, border, PillShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Int? = null,
) {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    Button(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier
            .height(YtDimens.PrimaryButtonHeight)
            .pressScale(source),
        enabled = enabled,
        shape = PillShape,
        interactionSource = source,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = YtTheme.colors.tonal,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            YtIcon(icon)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    Button(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier
            .height(YtDimens.PrimaryButtonHeight)
            .pressScale(source),
        shape = PillShape,
        interactionSource = source,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun OutlinePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    OutlinedButton(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier
            .height(YtDimens.PrimaryButtonHeight)
            .pressScale(source),
        enabled = enabled,
        shape = PillShape,
        interactionSource = source,
        border = BorderStroke(YtDimens.Hairline, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun YtProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by rememberAnimatedProgress(progress)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(PillShape)
            .background(YtTheme.colors.track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.primary, PillShape),
        )
    }
}

@Composable
fun DownloadRow(item: DownloadItem, modifier: Modifier = Modifier, onShare: (() -> Unit)? = null) {
    YtCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(
                start = 10.dp,
                top = 10.dp,
                bottom = 10.dp,
                end = if (onShare != null) 6.dp else 14.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconTile(item.format.icon)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (onShare != null) {
                RoundIconButton(R.drawable.ic_share, "Share", onShare)
            }
        }
    }
}

@Composable
fun RoundIconButton(@DrawableRes icon: Int, description: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    IconButton(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = Modifier
            .size(YtDimens.MinTouch)
            .pressScale(source),
        interactionSource = source,
    ) {
        YtIcon(icon, tint = MaterialTheme.colorScheme.onSurfaceVariant, contentDescription = description)
    }
}

// one download in the queue (running, waiting or failed)
@Composable
fun QueueJobCard(
    job: QueueJob,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val failed = job.status == JobStatus.Failed
    val border = when {
        failed -> MaterialTheme.colorScheme.error
        job.status == JobStatus.Running -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val statusText = when {
        failed -> job.error ?: "Something went wrong."
        job.status == JobStatus.Queued -> "Waiting for a free slot"
        job.step == JobStep.Preparing -> "Getting ready"
        job.step == JobStep.Downloading -> "Downloading · ${(job.progress * 100).toInt()}%"
        job.step == JobStep.Converting -> "Converting"
        else -> "Saving to your phone"
    }
    YtCard(modifier = modifier.fillMaxWidth().animateContentSize(motionSpring()), borderColor = border) {
        Column(
            modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 6.dp, bottom = if (failed) 10.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconTile(job.format.icon)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        job.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            failed -> MaterialTheme.colorScheme.error
                            job.status == JobStatus.Running -> YtTheme.colors.goldText
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        // show the whole error when it failed so its possible to see what went wrong
                        maxLines = if (failed) 8 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (failed) {
                    RoundIconButton(R.drawable.ic_retry, "Try again", onRetry)
                    RoundIconButton(R.drawable.ic_close, "Dismiss", onDismiss)
                } else {
                    RoundIconButton(R.drawable.ic_close, "Cancel", onCancel)
                }
            }
            if (!failed) {
                YtProgressBar(job.progress, Modifier.padding(start = 4.dp, end = 8.dp))
            }
        }
    }
}

// small pill at the bottom, shows for a moment
@Composable
fun ToastPill(text: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondary, PillShape)
            .height(48.dp)
            .padding(start = 20.dp, end = if (actionLabel != null) 8.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondary)
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .height(YtDimens.MinTouch)
                    .ytClickable(onClick = onAction)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    actionLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    }
}

// slides the toast in and out. its in its own function because AnimatedVisibility
// inside a Box inside a Column picks the wrong version and wont compile
@Composable
fun ToastHost(
    visible: Boolean,
    text: String,
    actionLabel: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = LocalMotionEnabled.current
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(ytTween(motion)) + slideInVertically(ytSpring(motion)) { it / 2 },
        exit = fadeOut(ytTween(motion, Motion.Micro + 30)) + slideOutVertically(ytSpring(motion)) { it / 2 },
    ) {
        ToastPill(text = text, actionLabel = actionLabel, onAction = onAction)
    }
}

enum class Tab { Download, Library, Settings }

@Composable
fun YtBottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        HorizontalDivider(thickness = YtDimens.Hairline, color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BottomBarItem("Download", R.drawable.ic_download, selected == Tab.Download, Modifier.weight(1f)) {
                onSelect(Tab.Download)
            }
            BottomBarItem("Library", R.drawable.ic_folder, selected == Tab.Library, Modifier.weight(1f)) {
                onSelect(Tab.Library)
            }
            BottomBarItem("Settings", R.drawable.ic_settings, selected == Tab.Settings, Modifier.weight(1f)) {
                onSelect(Tab.Settings)
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: Int,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        if (selected) YtTheme.colors.goldText else MaterialTheme.colorScheme.onSurfaceVariant,
        motionTween(),
        label = "tabColor",
    )
    Column(
        modifier = modifier
            .height(48.dp)
            .ytClickable(onClick = onClick)
            .clip(PillShape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        YtIcon(icon, tint = color, size = 22.dp)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
