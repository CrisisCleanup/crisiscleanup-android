package com.crisiscleanup.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.crisiscleanup.core.designsystem.R

val Roboto = FontFamily.Default
val NunitoSans = FontFamily(
    Font(R.font.nunito_sans, FontWeight.Normal),
    Font(R.font.nunito_sans, FontWeight.W400),
    Font(R.font.nunito_sans_italic, style = FontStyle.Italic),
    Font(R.font.nunito_sans_bold, FontWeight.Bold),
    Font(R.font.nunito_sans_bold, FontWeight.W700),
    Font(R.font.nunito_sans_bold_italic, FontWeight.Bold, style = FontStyle.Italic),
)

data class CrisisCleanupTypography(
    val linkTextStyle: TextStyle,
    val accentTextStyle: TextStyle,
    val helpTextStyle: TextStyle,
    val header1: TextStyle,
    val header2: TextStyle,
    val header3: TextStyle,
    val header4: TextStyle,
    val header5: TextStyle,
)

val CrisisCleanupTypographyDefault = CrisisCleanupTypography(
    linkTextStyle = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 15.8.sp,
        fontSize = 16.sp
    ),
    accentTextStyle = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W600,
        letterSpacing = 0.sp,
        lineHeight = 21.82.sp,
        fontSize = 16.sp
    ),
    helpTextStyle = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 16.37.sp,
        fontSize = 12.sp
    ),
    header1 = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 28.7.sp,
        fontSize = 24.sp
    ),
    header2 = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 27.28.sp,
        fontSize = 20.sp
    ),
    header3 = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 24.55.sp,
        fontSize = 18.sp
    ),
    header4 = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
        fontSize = 16.sp
    ),
    header5 = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W700,
        letterSpacing = 0.sp,
        lineHeight = 16.sp,
        fontSize = 14.sp
    )
)

val LocalFontStyles = staticCompositionLocalOf { CrisisCleanupTypographyDefault }

internal val AppTypography = Typography(
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.10000000149011612.sp,
        lineHeight = 16.sp,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.10000000149011612.sp,
        lineHeight = 16.sp,
        fontSize = 11.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 21.82.sp,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NunitoSans,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.10000000149011612.sp,
        lineHeight = 16.8.sp,
        fontSize = 14.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 40.sp,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 36.sp,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 32.sp,
        fontSize = 24.sp
    ),
    displayLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 64.sp,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 52.sp,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 44.sp,
        fontSize = 36.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.sp,
        lineHeight = 28.sp,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 24.sp,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
        fontSize = 14.sp
    ),
)