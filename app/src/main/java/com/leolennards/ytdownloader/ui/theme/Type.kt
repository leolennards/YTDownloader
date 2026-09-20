package com.leolennards.ytdownloader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.leolennards.ytdownloader.R

// one variable font file (res/font/manrope.ttf), the weight is picked with variation settings
@OptIn(ExperimentalTextApi::class)
val Manrope = FontFamily(
    Font(
        R.font.manrope,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
)

private val Regular = FontWeight.Normal
private val SemiBold = FontWeight.SemiBold

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

// sizes come from the mockup. only regular and semibold used
val YtTypography = Typography(
    displayLarge = style(44, 50, SemiBold, -0.9),    // progress percentage
    displayMedium = style(36, 42, SemiBold, -0.7),
    displaySmall = style(32, 37, SemiBold, -0.6),
    headlineLarge = style(32, 37, SemiBold, -0.6),   // screen titles
    headlineMedium = style(24, 30, SemiBold, -0.3),
    headlineSmall = style(20, 25, SemiBold, -0.2),
    titleLarge = style(20, 25, SemiBold, -0.2),      // video title on preview
    titleMedium = style(16, 22, SemiBold),           // section headers
    titleSmall = style(15, 20, SemiBold),            // list row titles
    bodyLarge = style(16, 24, Regular),              // input text
    bodyMedium = style(15, 21, Regular),             // subtitles
    bodySmall = style(13, 18, Regular),              // row metadata
    labelLarge = style(14, 20, SemiBold),            // buttons
    labelMedium = style(12, 16, SemiBold),           // field labels
    labelSmall = style(11, 14, SemiBold),            // nav labels
)

// small label above screen titles. use .uppercase() on the text where its used
val EyebrowStyle = style(12, 16, SemiBold, 1.2)
