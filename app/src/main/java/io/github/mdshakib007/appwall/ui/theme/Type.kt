package io.github.mdshakib007.appwall.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System font only (no bundled font files) keeps the APK small and honours the user's font settings.
private val Sans = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = Sans, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displayMedium = Typography().displayMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    displaySmall = Typography().displaySmall.copy(fontFamily = Sans, fontWeight = FontWeight.Bold),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = Sans, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Bold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    titleSmall = Typography().titleSmall.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = Sans),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = Sans),
    bodySmall = Typography().bodySmall.copy(fontFamily = Sans),
    labelLarge = Typography().labelLarge.copy(fontFamily = Sans, fontWeight = FontWeight.SemiBold),
    labelMedium = Typography().labelMedium.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
    labelSmall = Typography().labelSmall.copy(fontFamily = Sans, fontWeight = FontWeight.Medium),
)
