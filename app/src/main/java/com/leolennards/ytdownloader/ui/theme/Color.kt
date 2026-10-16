package com.leolennards.ytdownloader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// ---- light: soft paper white, ink, amber ----
val LightBackground = Color(0xFFF4F3EF)
val LightSurface = Color(0xFFFFFFFF)      // cards
val LightSurfaceHigh = Color(0xFFEAE8E2)  // fields, raised bits inside cards
val LightTrack = Color(0xFFE4E1D9)        // toggle and progress track
val LightTonal = Color(0xFFFCEFD4)        // soft amber, selected chips and icon tiles
val LightPlaceholder = Color(0xFFE7E4DC)
val LightHairline = Color(0xFFE2DFD7)
val LightInk = Color(0xFF121212)
val LightMuted = Color(0xFF66635C)
val LightAccent = Color(0xFFF2A81D)       // buttons and progress
val LightAccentText = Color(0xFF8A5800)   // amber for text and icons (dark enough to read on white)

// ---- dark: deep graphite, warm white, bright amber ----
val DarkBackground = Color(0xFF0B0B0D)
val DarkSurface = Color(0xFF151518)
val DarkSurfaceHigh = Color(0xFF1F1F23)
val DarkTrack = Color(0xFF2A2A2F)
val DarkTonal = Color(0xFF2E2414)
val DarkPlaceholder = Color(0xFF232327)
val DarkHairline = Color(0xFF242428)
val DarkInk = Color(0xFFF5F3EE)
val DarkMuted = Color(0xFF9C9990)
val DarkAccent = Color(0xFFFFB938)
val DarkAccentText = Color(0xFFFFC55C)

val OnAccent = Color(0xFF1B1205)

val ErrorLight = Color(0xFFB3261E)
val ErrorDark = Color(0xFFFF8A80)
val SuccessLight = Color(0xFF2E7D4F)
val SuccessDark = Color(0xFF7BD8A0)

// extra colors that material3 has no slot for, use YtTheme.colors
@Immutable
data class YtExtraColors(
    val accent: Color,
    val accentText: Color,
    val tonal: Color,
    val track: Color,
    val hairline: Color,
    val placeholder: Color,
    val surfaceHigh: Color,
    val success: Color,
)

val LightExtraColors = YtExtraColors(
    accent = LightAccent,
    accentText = LightAccentText,
    tonal = LightTonal,
    track = LightTrack,
    hairline = LightHairline,
    placeholder = LightPlaceholder,
    surfaceHigh = LightSurfaceHigh,
    success = SuccessLight,
)

val DarkExtraColors = YtExtraColors(
    accent = DarkAccent,
    accentText = DarkAccentText,
    tonal = DarkTonal,
    track = DarkTrack,
    hairline = DarkHairline,
    placeholder = DarkPlaceholder,
    surfaceHigh = DarkSurfaceHigh,
    success = SuccessDark,
)
