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
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.manrope,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(750)),
    ),
)

private val Regular = FontWeight.Normal
private val Medium = FontWeight.Medium
private val SemiBold = FontWeight.SemiBold
private val Bold = FontWeight.Bold

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

// bold for big titles, semibold for anything you tap or scan, regular for reading
val YtTypography = Typography(
    displayLarge = style(56, 60, Bold, -1.6),        // progress percentage
    displayMedium = style(40, 46, Bold, -1.0),
    displaySmall = style(34, 40, Bold, -0.8),
    headlineLarge = style(34, 40, Bold, -0.9),       // screen titles
    headlineMedium = style(26, 32, Bold, -0.5),
    headlineSmall = style(22, 28, Bold, -0.3),       // player title
    titleLarge = style(20, 26, SemiBold, -0.2),      // video title on preview
    titleMedium = style(16, 22, SemiBold, -0.1),     // section headers, buttons
    titleSmall = style(15, 20, SemiBold),            // list row titles
    bodyLarge = style(16, 24, Regular),              // input text
    bodyMedium = style(15, 21, Regular),             // subtitles
    bodySmall = style(13, 18, Medium),               // row metadata
    labelLarge = style(14, 20, SemiBold),            // small buttons
    labelMedium = style(12, 16, SemiBold),           // field labels, badges
    labelSmall = style(11, 14, SemiBold, 0.2),       // nav labels
)

// small label above screen titles. use .uppercase() on the text where its used
val EyebrowStyle = style(12, 16, Bold, 1.4)
