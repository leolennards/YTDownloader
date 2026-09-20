package com.leolennards.ytdownloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueJob
import com.leolennards.ytdownloader.ui.components.IconTile
import com.leolennards.ytdownloader.ui.components.InkButton
import com.leolennards.ytdownloader.ui.components.OutlinePillButton
import com.leolennards.ytdownloader.ui.components.ScreenHeader
import com.leolennards.ytdownloader.ui.components.YtCard
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.rememberAnimatedProgress
import com.leolennards.ytdownloader.ui.theme.staggeredEntrance
import com.leolennards.ytdownloader.ui.theme.ytClickable
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit
import kotlin.math.roundToInt

private val Steps = listOf("Fetching video info", "Downloading", "Converting", "Saving to your phone")

private enum class StepState { Done, Current, Pending }

@Composable
fun DownloadingScreen(
    job: QueueJob?,
    onCancel: () -> Unit,
    onAddAnother: () -> Unit,
    onViewLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val failed = job?.status == JobStatus.Failed
    val done = job?.status == JobStatus.Done
    val active = job != null && job.isActive
    val progress = when {
        job == null -> 0f
        done -> 1f
        else -> job.progress
    }
    val current = when {
        job == null -> 0
        done -> Steps.size
        job.status == JobStatus.Queued -> 0
        else -> job.step.ordinal
    }
    val format = job?.format ?: MediaFormat.MP4
    val title = job?.title ?: "Video"
    val meta = when {
        job == null -> ""
        done -> job.result?.meta ?: ""
        job.status == JobStatus.Queued -> "Waiting for a free slot"
        else -> listOfNotNull(job.format.label, job.quality).joinToString(" · ")
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = YtDimens.ScreenPadding)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
        ) {
            ScreenHeader(
                eyebrow = when {
                    done -> "Finished"
                    failed -> "Problem"
                    else -> "In progress"
                },
                title = when {
                    done -> "Saved"
                    failed -> "Download failed"
                    else -> "Downloading"
                },
                subtitle = when {
                    done -> "Your file is in the library."
                    failed -> job?.error ?: "Something went wrong."
                    else -> "You can leave this screen. It keeps going in the background."
                },
            )

            AnimatedVisibility(visible = !failed, enter = ytEnter(), exit = ytExit()) {
                Column(verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .staggeredEntrance(0, "downloading-ring"),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProgressRing(progress, done)
                    }

                    YtCard(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            IconTile(format.icon)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (title == job?.url) "Getting the title" else title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    meta,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    YtCard(Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Steps.forEachIndexed { index, label ->
                                val state = when {
                                    index < current -> StepState.Done
                                    index == current -> StepState.Current
                                    else -> StepState.Pending
                                }
                                StepRow(label, state)
                            }
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = YtDimens.ScreenPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AnimatedVisibility(visible = active, enter = ytEnter(), exit = ytExit()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(YtDimens.MinTouch)
                        .ytClickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Cancel this download",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinePillButton("Add another", onClick = onAddAnother, modifier = Modifier.weight(1f))
                InkButton("View library", onClick = onViewLibrary, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, done: Boolean) {
    val animated by rememberAnimatedProgress(progress)
    val track = YtTheme.colors.track
    val gold = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = gold,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${(animated * 100).roundToInt()}%",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (done) "Complete" else "Downloading",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StepRow(label: String, state: StepState) {
    val motion = LocalMotionEnabled.current
    val fill by animateColorAsState(
        when (state) {
            StepState.Done -> MaterialTheme.colorScheme.primary
            StepState.Current -> YtTheme.colors.tonal
            StepState.Pending -> Color.Transparent
        },
        motionTween(),
        label = "stepFill",
    )
    val ring by animateColorAsState(
        when (state) {
            StepState.Pending -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.primary
        },
        motionTween(),
        label = "stepRing",
    )
    val check by animateFloatAsState(
        targetValue = if (state == StepState.Done) 1f else 0f,
        animationSpec = motionSpring(dampingRatio = 0.55f),
        label = "stepCheck",
    )
    val text by animateColorAsState(
        if (state == StepState.Current) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        motionTween(),
        label = "stepText",
    )
    // current step pulses so you can tell its working
    val pulse by rememberInfiniteTransition(label = "stepPulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stepPulseValue",
    )

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { alpha = if (state == StepState.Current && motion) pulse else 1f }
                .clip(PillShape)
                .background(fill)
                .border(2.dp, ring, PillShape),
            contentAlignment = Alignment.Center,
        ) {
            YtIcon(
                R.drawable.ic_check,
                tint = MaterialTheme.colorScheme.onPrimary,
                size = 14.dp,
                modifier = Modifier.graphicsLayer {
                    scaleX = check
                    scaleY = check
                    alpha = check.coerceIn(0f, 1f)
                },
            )
        }
        Text(
            label,
            style = if (state == StepState.Current) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            color = text,
        )
    }
}
