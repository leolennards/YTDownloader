package com.leolennards.ytdownloader.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    color: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border by animateColorAsState(borderColor, motionTween(), label = "cardBorder")
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = color,
        border = BorderStroke(YtDimens.Hairline, border),
    ) {
        Column(content = content)
    }
}

@Composable
fun IconTile(@DrawableRes icon: Int, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(YtTheme.colors.tonal, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        YtIcon(icon, tint = YtTheme.colors.accentText)
    }
}

@Composable
fun EyebrowLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = EyebrowStyle, color = YtTheme.colors.accentText)
}

// screen title, fades when the text changes. trailing sits at the end of the title row
@Composable
fun ScreenHeader(
    eyebrow: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val motion = LocalMotionEnabled.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AnimatedContent(
                targetState = eyebrow,
                transitionSpec = { fadeIn(ytTween(motion)) togetherWith fadeOut(ytTween(motion, Motion.Micro)) },
                label = "eyebrow",
            ) { text ->
                EyebrowLabel(text)
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
        trailing?.invoke(this)
    }
}

// title on the left of a list section, optional text action on the right
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().height(YtDimens.MinTouch),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (actionLabel != null) TextAction(actionLabel, onAction)
    }
}

// a tappable word, like "See all" or "Cancel all"
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = YtTheme.colors.accentText,
) {
    Box(
        modifier = modifier
            .height(YtDimens.MinTouch)
            .clip(PillShape)
            .ytClickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = color)
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
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(YtTheme.colors.surfaceHigh, PillShape)
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
                .height(YtDimens.MinTouch - 4.dp)
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
                        .height(YtDimens.MinTouch - 4.dp)
                        .clip(PillShape)
                        .ytClickable(haptic = false) {
                            if (!selected) haptics.tick()
                            onSelect(index)
                        }
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                        },
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
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        motionTween(),
        label = "pillBorder",
    )
    val background by animateColorAsState(
        if (selected) YtTheme.colors.tonal else YtTheme.colors.surfaceHigh,
        motionTween(),
        label = "pillBackground",
    )
    val content by animateColorAsState(
        if (selected) YtTheme.colors.accentText else MaterialTheme.colorScheme.onSurface,
        motionTween(),
        label = "pillText",
    )
    Box(
        modifier = modifier
            .height(YtDimens.MinTouch - 4.dp)
            .clip(PillShape)
            .ytClickable(onClick = onClick)
            .background(background)
            .border(1.5.dp, border, PillShape)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

// the main action on a screen, amber
@Composable
fun PrimaryButton(
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
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = YtTheme.colors.surfaceHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            YtIcon(icon)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// strong but not the main action, ink colored
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null) {
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
        if (icon != null) {
            YtIcon(icon)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1)
    }
}

// quiet button on a soft fill
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Int? = null,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
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
            containerColor = YtTheme.colors.surfaceHigh,
            contentColor = contentColor,
            disabledContainerColor = YtTheme.colors.surfaceHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        if (icon != null) {
            YtIcon(icon)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun YtProgressBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val animated by rememberAnimatedProgress(progress)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
            .background(YtTheme.colors.track),
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .height(height)
                .background(MaterialTheme.colorScheme.primary, PillShape),
        )
    }
}

// one saved file: cover, title, artist and details, and a menu with share and delete
@Composable
fun DownloadRow(
    item: DownloadItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    onClick: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.ytClickable(pressedScale = 0.98f, onClick = onClick) else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Artwork(item.uri, item.format, Modifier.size(YtDimens.RowArt))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (playing) YtTheme.colors.accentText else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(item.artist?.takeIf { it.isNotBlank() }, item.meta).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onShare != null || onDelete != null) {
            RowMenu(onShare = onShare, onDelete = onDelete)
        }
    }
}

@Composable
private fun RowMenu(onShare: (() -> Unit)?, onDelete: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box {
        RoundIconButton(R.drawable.ic_more, "More options", onClick = { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = MaterialTheme.shapes.small,
            containerColor = YtTheme.colors.surfaceHigh,
        ) {
            if (onShare != null) {
                DropdownMenuItem(
                    text = { Text("Share", style = MaterialTheme.typography.bodyLarge) },
                    leadingIcon = { YtIcon(R.drawable.ic_share) },
                    onClick = {
                        open = false
                        onShare()
                    },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Delete", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { YtIcon(R.drawable.ic_delete, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        open = false
                        onDelete()
                    },
                )
            }
        }
    }
}

@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    background: Color = Color.Transparent,
) {
    val source = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    IconButton(
        onClick = {
            haptics.tick()
            onClick()
        },
        modifier = modifier
            .size(YtDimens.MinTouch)
            .pressScale(source)
            .background(background, PillShape),
        interactionSource = source,
    ) {
        YtIcon(icon, tint = tint, contentDescription = description)
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
    val statusText = when {
        failed -> job.error ?: "Something went wrong."
        job.status == JobStatus.Queued && job.waitingForWifi -> "Waiting for Wi-Fi"
        job.status == JobStatus.Queued -> "Waiting for a free slot"
        job.step == JobStep.Preparing -> "Getting ready"
        job.step == JobStep.Downloading -> "Downloading · ${(job.progress * 100).toInt()}%"
        job.step == JobStep.Converting -> "Converting"
        else -> "Saving to your phone"
    }.let { text -> if (!failed && !job.artist.isNullOrBlank()) "${job.artist} · $text" else text }
    YtCard(
        modifier = modifier.fillMaxWidth().animateContentSize(motionSpring()),
        borderColor = if (failed) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, end = 6.dp, bottom = if (failed) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RemoteArtwork(job.thumbnailUrl, job.format, Modifier.size(48.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        if (job.title == job.url) "Getting the title" else job.title,
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
                            job.status == JobStatus.Running -> YtTheme.colors.accentText
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        // show the whole error when it failed so its possible to see what went wrong
                        maxLines = if (failed) 8 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (failed) {
                    RoundIconButton(R.drawable.ic_retry, "Try again", onRetry, tint = YtTheme.colors.accentText)
                    RoundIconButton(R.drawable.ic_close, "Dismiss", onDismiss)
                } else {
                    RoundIconButton(R.drawable.ic_close, "Cancel", onCancel)
                }
            }
            if (!failed) {
                YtProgressBar(job.progress, Modifier.padding(start = 2.dp, end = 10.dp), height = 4.dp)
            }
        }
    }
}

// a friendly message for an empty list, with an optional button
@Composable
fun EmptyState(
    @DrawableRes icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon, size = 64.dp)
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null) {
            Spacer(Modifier.height(6.dp))
            SecondaryButton(actionLabel, onClick = onAction, contentColor = YtTheme.colors.accentText)
        }
    }
}

// small pill at the bottom, shows for a moment
@Composable
fun ToastPill(text: String, actionLabel: String?, onAction: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .shadow(12.dp, PillShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(MaterialTheme.colorScheme.secondary, PillShape)
            .height(52.dp)
            .padding(start = 20.dp, end = if (actionLabel != null) 6.dp else 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (actionLabel != null) {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(PillShape)
                    .ytClickable(onClick = onAction)
                    .background(MaterialTheme.colorScheme.primary, PillShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
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

// floating pill nav bar, the soft amber pill slides to the open tab.
// badge shows how many downloads are running on the library tab
@Composable
fun YtBottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier, badge: Int = 0) {
    val tabs = listOf(
        Triple(Tab.Download, "Download", R.drawable.ic_download),
        Triple(Tab.Library, "Library", R.drawable.ic_folder),
        Triple(Tab.Settings, "Settings", R.drawable.ic_settings),
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .shadow(20.dp, PillShape, ambientColor = Color.Black, spotColor = Color.Black)
                .background(YtTheme.colors.surfaceHigh, PillShape)
                .border(YtDimens.Hairline, MaterialTheme.colorScheme.outline, PillShape)
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / tabs.size
            val index = tabs.indexOfFirst { it.first == selected }
            val x by animateDpAsState(itemWidth * index, motionSpring(), label = "navIndicator")
            Box(
                Modifier
                    .offset(x = x)
                    .width(itemWidth)
                    .height(52.dp)
                    .background(YtTheme.colors.tonal, PillShape),
            )
            Row {
                tabs.forEach { (tab, label, icon) ->
                    BottomBarItem(
                        label = label,
                        icon = icon,
                        selected = tab == selected,
                        badge = if (tab == Tab.Library) badge else 0,
                        modifier = Modifier.width(itemWidth),
                    ) { onSelect(tab) }
                }
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    label: String,
    icon: Int,
    selected: Boolean,
    badge: Int,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(
        if (selected) YtTheme.colors.accentText else MaterialTheme.colorScheme.onSurfaceVariant,
        motionTween(),
        label = "tabColor",
    )
    Column(
        modifier = modifier
            .height(52.dp)
            .clip(PillShape)
            .ytClickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
                role = Role.Tab
                if (badge > 0) contentDescription = "$label, $badge downloading"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            YtIcon(icon, tint = color, size = 22.dp)
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(16.dp)
                        .background(MaterialTheme.colorScheme.primary, PillShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (badge > 9) "9+" else badge.toString(),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
