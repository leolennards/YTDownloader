package com.leolennards.ytdownloader.ui.theme

import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

// animation helpers. springs for movement, fades for fading
// all animations are skipped if animations are off in system settings

object Motion {
    const val Micro = 120       // button and toggle feedback
    const val Fade = 250        // fades
    const val Standard = 300    // most transitions
    const val Screen = 360      // big screen transitions
    const val Progress = 700    // rings and bars filling up
    const val CountUp = 800     // numbers counting up
    const val Stagger = 50      // delay between list items

    val ScreenSlide: Dp = 24.dp // how far screens slide
    val ListRise: Dp = 12.dp    // how far list items rise
    const val PressedScale = 0.97f
}

// ---- animations off setting ----

// false if the user turned system animations off
val LocalMotionEnabled = staticCompositionLocalOf { true }

private fun readMotionEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
}.getOrDefault(true)

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(readMotionEnabled(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled = readMotionEnabled(context)
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer,
            )
        }
        onDispose { runCatching { context.contentResolver.unregisterContentObserver(observer) } }
    }
    return enabled
}

// ---- animation specs ----

fun <T> ytSpring(
    enabled: Boolean = true,
    dampingRatio: Float = 0.85f,
    stiffness: Float = Spring.StiffnessMediumLow,
): FiniteAnimationSpec<T> =
    if (enabled) spring(dampingRatio = dampingRatio, stiffness = stiffness) else snap()

fun <T> ytTween(
    enabled: Boolean = true,
    durationMillis: Int = Motion.Fade,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
): FiniteAnimationSpec<T> =
    if (enabled) tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing) else snap()

@Composable
fun <T> motionSpring(
    dampingRatio: Float = 0.85f,
    stiffness: Float = Spring.StiffnessMediumLow,
): FiniteAnimationSpec<T> = ytSpring(LocalMotionEnabled.current, dampingRatio, stiffness)

@Composable
fun <T> motionTween(
    durationMillis: Int = Motion.Fade,
    delayMillis: Int = 0,
    easing: Easing = FastOutSlowInEasing,
): FiniteAnimationSpec<T> = ytTween(LocalMotionEnabled.current, durationMillis, delayMillis, easing)

// standard appear and disappear for things that take up space
@Composable
fun ytEnter(): EnterTransition = fadeIn(motionTween()) + expandVertically(motionSpring())

@Composable
fun ytExit(): ExitTransition = fadeOut(motionTween(Motion.Micro + 30)) + shrinkVertically(motionSpring())

// ---- vibration ----

class Haptics internal constructor(private val view: View) {
    fun tick() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun confirm() {
        val constant = if (Build.VERSION.SDK_INT >= 30) {
            HapticFeedbackConstants.CONFIRM
        } else {
            HapticFeedbackConstants.CONTEXT_CLICK
        }
        view.performHapticFeedback(constant)
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

// ---- press feedback ----

// shrinks a bit while pressed
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = Motion.PressedScale,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motionSpring(dampingRatio = 0.9f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// clickable that shrinks when pressed and vibrates a little. put it before clip and background
@Composable
fun Modifier.ytClickable(
    enabled: Boolean = true,
    haptic: Boolean = true,
    pressedScale: Float = Motion.PressedScale,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = rememberHaptics()
    return this
        .pressScale(interactionSource, pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = Role.Button,
        ) {
            if (haptic) haptics.tick()
            onClick()
        }
}

// ---- numbers, progress and lists ----

// counts up from 0 the first time, then moves to new values
@Composable
fun rememberCountUp(target: Float, durationMillis: Int = Motion.CountUp): State<Float> {
    val enabled = LocalMotionEnabled.current
    val animatable = remember { Animatable(if (enabled) 0f else target) }
    LaunchedEffect(target, enabled) {
        if (enabled) {
            animatable.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing))
        } else {
            animatable.snapTo(target)
        }
    }
    return animatable.asState()
}

@Composable
fun rememberCountUpInt(target: Int, durationMillis: Int = Motion.CountUp): State<Int> {
    val animated = rememberCountUp(target.toFloat(), durationMillis)
    return remember(animated) { derivedStateOf { animated.value.roundToInt() } }
}

// progress from 0 to 1 for the bars and rings
@Composable
fun rememberAnimatedProgress(target: Float): State<Float> {
    val enabled = LocalMotionEnabled.current
    val animatable = remember { Animatable(if (enabled) 0f else target.coerceIn(0f, 1f)) }
    var firstDone by remember { mutableStateOf(!enabled) }
    LaunchedEffect(target, enabled) {
        val clamped = target.coerceIn(0f, 1f)
        when {
            !enabled -> animatable.snapTo(clamped)
            !firstDone -> {
                firstDone = true
                animatable.animateTo(clamped, tween(Motion.Progress, easing = EaseOutCubic))
            }
            else -> animatable.animateTo(clamped, spring(dampingRatio = 0.9f, stiffness = Spring.StiffnessLow))
        }
    }
    return animatable.asState()
}

// remembers which list animations already played
object EntranceMemory {
    val played = HashSet<String>()
}

// fades a list item in and moves it up, small delay for each item. only plays once
@Composable
fun Modifier.staggeredEntrance(index: Int, key: String? = null, rise: Dp = Motion.ListRise): Modifier {
    val enabled = LocalMotionEnabled.current
    var playedLocal by rememberSaveable { mutableStateOf(false) }
    val alreadyPlayed = playedLocal || (key != null && key in EntranceMemory.played)
    val progress = remember { Animatable(if (alreadyPlayed || !enabled) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!alreadyPlayed && enabled) {
            delay((index.coerceIn(0, 8) * Motion.Stagger).milliseconds)
            progress.animateTo(1f, tween(Motion.Standard, easing = FastOutSlowInEasing))
        }
        playedLocal = true
        if (key != null) EntranceMemory.played.add(key)
    }
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * risePx
    }
}
