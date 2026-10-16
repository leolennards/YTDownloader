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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.R
import com.leolennards.ytdownloader.data.DownloadItem
import com.leolennards.ytdownloader.data.JobStatus
import com.leolennards.ytdownloader.data.MediaFormat
import com.leolennards.ytdownloader.data.QueueJob
import com.leolennards.ytdownloader.ui.components.EyebrowLabel
import com.leolennards.ytdownloader.ui.components.InkButton
import com.leolennards.ytdownloader.ui.components.PrimaryButton
import com.leolennards.ytdownloader.ui.components.RemoteArtwork
import com.leolennards.ytdownloader.ui.components.SecondaryButton
import com.leolennards.ytdownloader.ui.components.TextAction
import com.leolennards.ytdownloader.ui.components.YtIcon
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtDimens
import com.leolennards.ytdownloader.ui.theme.YtTheme
import com.leolennards.ytdownloader.ui.theme.motionSpring
import com.leolennards.ytdownloader.ui.theme.motionTween
import com.leolennards.ytdownloader.ui.theme.rememberAnimatedProgress
import com.leolennards.ytdownloader.ui.theme.ytEnter
import com.leolennards.ytdownloader.ui.theme.ytExit
import kotlin.math.roundToInt

private val Steps = listOf("Details", "Download", "Convert", "Save")

private enum class StepState { Done, Current, Pending }

@Composable
fun DownloadingScreen(
    job: QueueJob?,
    onCancel: () -> Unit,
    onAddAnother: () -> Unit,
    onViewLibrary: () -> Unit,
    onPlay: (DownloadItem) -> Unit,
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
    val title = job?.title?.takeIf { it != job.url } ?: "Getting the title"
    val status = when {
        job == null -> ""
        done -> "Saved to your phone"
        failed -> "Download failed"
        job.waitingForWifi -> "Waiting for Wi-Fi"
        job.status == JobStatus.Queued -> "Waiting for a free slot"
        else -> listOf("Getting the details", "Downloading", "Converting", "Saving to your phone")[job.step.ordinal]
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = YtDimens.ScreenPadding)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YtDimens.SectionGap),
        ) {
            EyebrowLabel(
                when {
                    done -> "Finished"
                    failed -> "Problem"
                    else -> "In progress"
                },
            )

            // the artwork inside the progress ring
            ArtRing(progress = progress, done = done, failed = failed) {
                RemoteArtwork(
                    job?.thumbnailUrl,
                    format,
                    Modifier.fillMaxSize(),
                    shape = PillShape,
                    loadSize = 400.dp,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = listOfNotNull(job?.artist?.takeIf { it.isNotBlank() }, job?.format?.label, job?.quality)
                    .joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val animated by rememberAnimatedProgress(progress)
                if (!failed) {
                    Text(
                        "${(animated * 100).roundToInt()}%",
                        style = MaterialTheme.typography.displayLarge,
                        color = if (done) YtTheme.colors.success else MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    status,
                    style = MaterialTheme.typography.titleSmall,
                    color = when {
                        failed -> MaterialTheme.colorScheme.error
                        done -> YtTheme.colors.success
                        else -> YtTheme.colors.accentText
                    },
                )
                if (failed) {
                    Text(
                        job?.error ?: "Something went wrong.",
                        style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            AnimatedVisibility(visible = !failed, enter = ytEnter(), exit = ytExit()) {
                Stepper(current)
            }

            AnimatedVisibility(visible = active, enter = ytEnter(), exit = ytExit()) {
                Text(
                    "You can leave this screen, it keeps going in the background.",
                    style = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = YtDimens.ScreenPadding, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val result = job?.result
            AnimatedVisibility(visible = done && result != null, enter = ytEnter(), exit = ytExit()) {
                PrimaryButton(
                    "Play it now",
                    onClick = { result?.let(onPlay) },
                    icon = R.drawable.ic_play,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryButton("Add another", onClick = onAddAnother, modifier = Modifier.weight(1f))
                InkButton("Library", onClick = onViewLibrary, modifier = Modifier.weight(1f))
            }
            AnimatedVisibility(visible = active, enter = ytEnter(), exit = ytExit()) {
                TextAction("Cancel this download", onCancel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// a round picture with the progress drawn around it
@Composable
private fun ArtRing(progress: Float, done: Boolean, failed: Boolean, content: @Composable () -> Unit) {
    val animated by rememberAnimatedProgress(progress)
    val track = YtTheme.colors.track
    val color by animateColorAsState(
        when {
            failed -> MaterialTheme.colorScheme.error
            done -> YtTheme.colors.success
            else -> MaterialTheme.colorScheme.primary
        },
        motionTween(),
        label = "ringColor",
    )
    val scale by animateFloatAsState(if (done) 1.04f else 1f, motionSpring(dampingRatio = 0.5f), label = "ringPop")
    Box(
        modifier = Modifier
            .size(236.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, -90f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(
                color,
                -90f,
                360f * (if (failed) 1f else animated),
                false,
                topLeft,
                arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Box(Modifier.size(196.dp).clip(PillShape)) {
            content()
            if (done) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .background(YtTheme.colors.success, PillShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        YtIcon(R.drawable.ic_check, tint = Color.Black, size = 28.dp, contentDescription = "Done")
                    }
                }
            }
        }
    }
}

// four dots joined by lines, with the step names under them
@Composable
private fun Stepper(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Steps.forEachIndexed { index, label ->
            val state = when {
                index < current -> StepState.Done
                index == current -> StepState.Current
                else -> StepState.Pending
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepLine(visible = index > 0, lit = index <= current, modifier = Modifier.weight(1f))
                    StepDot(state)
                    StepLine(visible = index < Steps.lastIndex, lit = index < current, modifier = Modifier.weight(1f))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (state == StepState.Pending) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StepLine(visible: Boolean, lit: Boolean, modifier: Modifier) {
    val color by animateColorAsState(
        when {
            !visible -> Color.Transparent
            lit -> MaterialTheme.colorScheme.primary
            else -> YtTheme.colors.track
        },
        motionTween(),
        label = "stepLine",
    )
    Box(modifier.height(2.dp).background(color))
}

@Composable
private fun StepDot(state: StepState) {
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
        if (state == StepState.Pending) YtTheme.colors.track else MaterialTheme.colorScheme.primary,
        motionTween(),
        label = "stepRing",
    )
    val check by animateFloatAsState(
        targetValue = if (state == StepState.Done) 1f else 0f,
        animationSpec = motionSpring(dampingRatio = 0.55f),
        label = "stepCheck",
    )
    // the current step pulses so you can tell its working
    val pulse by rememberInfiniteTransition(label = "stepPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "stepPulseValue",
    )
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
}
