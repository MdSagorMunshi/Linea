package com.ryanshelby.linea.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ryanshelby.linea.R

val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_regular, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_semibold, FontWeight.Bold)
)

private val ClassicTypography = Typography(
    // Headlines: ~28sp, Semibold
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    // Section titles: ~18sp, Semibold
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    // Body text: ~15sp, Regular
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp,
        color = LineaColors.TextSecondary,
        fontFeatureSettings = "tnum"
    ),
    // Captions: ~13sp, Regular
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
        color = LineaColors.TextSecondary,
        fontFeatureSettings = "tnum"
    ),
    // Dial Pad / Number Large Display: Tabular Numbers
    displayLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 1.sp,
        color = LineaColors.TextPrimary,
        fontFeatureSettings = "tnum"
    )
)

private val CrystalTypography = ClassicTypography.copy(
    displayLarge = ClassicTypography.displayLarge.copy(color = androidx.compose.ui.graphics.Color.Unspecified),
    headlineLarge = ClassicTypography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1).sp, color = androidx.compose.ui.graphics.Color.Unspecified),
    headlineMedium = ClassicTypography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-.7).sp, color = androidx.compose.ui.graphics.Color.Unspecified),
    titleLarge = ClassicTypography.titleLarge.copy(fontFamily = InterFontFamily, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-.5).sp, color = androidx.compose.ui.graphics.Color.Unspecified),
    titleMedium = ClassicTypography.titleMedium.copy(color = androidx.compose.ui.graphics.Color.Unspecified),
    titleSmall = ClassicTypography.titleSmall.copy(color = androidx.compose.ui.graphics.Color.Unspecified),
    bodyLarge = ClassicTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp, color = androidx.compose.ui.graphics.Color.Unspecified),
    bodyMedium = ClassicTypography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp, color = androidx.compose.ui.graphics.Color.Unspecified),
    bodySmall = TextStyle(fontFamily = InterFontFamily, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = ClassicTypography.labelSmall.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = .5.sp, color = androidx.compose.ui.graphics.Color.Unspecified)
)

val LineaTypography: Typography
    get() = if (LineaColors.isCrystal) CrystalTypography else ClassicTypography

val Typography.captionBold: TextStyle
    get() = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 0.5.sp,
        color = LineaColors.TextPrimary
    )

val Typography.captionMedium: TextStyle
    get() = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.2.sp,
        color = LineaColors.TextSecondary
    )

