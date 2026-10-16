package com.leolennards.ytdownloader.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.leolennards.ytdownloader.ui.theme.LocalMotionEnabled
import com.leolennards.ytdownloader.ui.theme.PillShape
import com.leolennards.ytdownloader.ui.theme.YtTheme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// ---- artwork that flies between screens ----

// set by the app around its screens, so any artwork can take part in the screen transition
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalScreenScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

// the same key on two screens makes the picture move from one to the other
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedArt(key: String?): Modifier {
    val shared = LocalSharedScope.current
    val screen = LocalScreenScope.current
    if (key == null || shared == null || screen == null || !LocalMotionEnabled.current) return this
    return with(shared) {
        this@sharedArt.sharedElement(
            rememberSharedContentState("art-$key"),
            animatedVisibilityScope = screen,
            boundsTransform = { _, _ -> spring(dampingRatio = 0.86f, stiffness = 380f) },
        )
    }
}

// ---- loading shimmer ----

// a soft light sweeping across, for things that are still loading
fun Modifier.shimmer(): Modifier = composed {
    val motion = LocalMotionEnabled.current
    val base = YtTheme.colors.surfaceHigh
    val shine = YtTheme.colors.track
    val x by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "shimmerX",
    )
    val offset = if (motion) x else 0.5f
    this.background(
        Brush.linearGradient(
            colors = listOf(base, shine, base),
            start = Offset(offset * 600f - 300f, 0f),
            end = Offset(offset * 600f + 300f, 0f),
        ),
    )
}

// grey rounded bar that stands in for a line of text while it loads
@Composable
fun SkeletonLine(width: Dp, height: Dp = 14.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(width, height)
            .clip(PillShape)
            .shimmer(),
    )
}

// ---- now playing bars ----

// three little bars that bounce while music plays, and rest when it is paused
@Composable
fun Equalizer(playing: Boolean, modifier: Modifier = Modifier, color: Color = YtTheme.colors.accent, height: Dp = 16.dp) {
    val motion = LocalMotionEnabled.current && playing
    val transition = rememberInfiniteTransition(label = "eq")
    val bars = listOf(520, 380, 640).mapIndexed { i, period ->
        transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(period, delayMillis = i * 90, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "eqBar$i",
        )
    }
    Row(
        modifier = modifier.size(width = 16.dp, height = height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEachIndexed { i, bar ->
            val rest = listOf(0.45f, 0.75f, 0.35f)[i]
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight(if (motion) bar.value else rest)
                    .background(color, PillShape),
            )
        }
    }
}

// ---- celebration ----

private class Piece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val round: Boolean,
)

// a burst of confetti from the middle, plays once each time key changes to something new
@Composable
fun ConfettiBurst(key: Any?, modifier: Modifier = Modifier) {
    if (key == null || !LocalMotionEnabled.current) return
    val colors = listOf(YtTheme.colors.accent, YtTheme.colors.success, Color.White, YtTheme.colors.accentText, Color(0xFFFF7A59))
    val pieces = remember(key) {
        val random = Random(key.hashCode())
        List(56) {
            Piece(
                angle = random.nextFloat() * 360f,
                speed = 0.45f + random.nextFloat() * 0.75f,
                spin = random.nextFloat() * 720f - 360f,
                size = 6f + random.nextFloat() * 8f,
                color = colors[random.nextInt(colors.size)],
                round = random.nextBoolean(),
            )
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(1600, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val center = Offset(size.width / 2, size.height / 2)
        val reach = size.minDimension * 0.75f
        pieces.forEach { p ->
            val rad = Math.toRadians(p.angle.toDouble())
            // flies out fast, slows down, then falls a little
            val travel = reach * p.speed * (1 - (1 - t) * (1 - t))
            val x = center.x + cos(rad).toFloat() * travel
            val y = center.y + sin(rad).toFloat() * travel + t * t * size.height * 0.35f
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(p.spin * t, Offset(x, y)) {
                if (p.round) {
                    drawCircle(p.color.copy(alpha = alpha), p.size / 2, Offset(x, y))
                } else {
                    drawRect(p.color.copy(alpha = alpha), Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
                }
            }
        }
    }
}
