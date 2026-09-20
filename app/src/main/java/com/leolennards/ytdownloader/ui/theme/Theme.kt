package com.leolennards.ytdownloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LightGold,
    onPrimary = LightInk,
    primaryContainer = LightTonal,
    onPrimaryContainer = LightGoldText,
    secondary = LightInk,                 // selected toggle: dark pill, cream text
    onSecondary = LightBackground,
    secondaryContainer = LightTonal,
    onSecondaryContainer = LightGoldText,
    tertiary = LightGoldText,
    onTertiary = LightBackground,
    background = LightBackground,
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightTrack,
    onSurfaceVariant = LightMuted,
    surfaceTint = Color.Transparent,
    outline = LightHairline,
    outlineVariant = LightHairline,
    error = ErrorLight,
    onError = Color.White,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightTrack,
    surfaceContainerHighest = LightTrack,
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGold,
    onPrimary = LightInk,
    primaryContainer = DarkTonal,
    onPrimaryContainer = DarkGoldText,
    secondary = DarkInk,
    onSecondary = DarkBackground,
    secondaryContainer = DarkTonal,
    onSecondaryContainer = DarkGoldText,
    tertiary = DarkGoldText,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkTrack,
    onSurfaceVariant = DarkMuted,
    surfaceTint = Color.Transparent,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    error = ErrorDark,
    onError = DarkBackground,
    surfaceContainerLowest = DarkSurface,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkTrack,
    surfaceContainerHighest = DarkTrack,
)

private val LocalYtColors = staticCompositionLocalOf { LightExtraColors }

// gives access to the extra colors, like YtTheme.colors.goldText
object YtTheme {
    val colors: YtExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalYtColors.current
}

@Composable
fun YTDownloaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // no dynamic color on purpose, the gold look is the design
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extra = if (darkTheme) DarkExtraColors else LightExtraColors

    CompositionLocalProvider(
        LocalYtColors provides extra,
        LocalMotionEnabled provides rememberMotionEnabled(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = YtTypography,
            shapes = YtShapes,
            content = content,
        )
    }
}
