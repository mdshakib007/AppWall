package io.github.mdshakib007.appwall.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Design language: neutral pages and pure white / deep-navy cards, navy or white text, and ONE accent:
 * brand orange. Orange appears on icons, primary buttons, selection and progress; never as a page tint.
 */
val BrandOrange = Color(0xFFF97316)   // tailwind orange-500
val BrandOrangeDeep = Color(0xFFEA580C)  // orange-600

// Light neutrals (cool grays + navy text)
private val L_Bg = Color(0xFFF8FAFC)      // slate-50
private val L_Card = Color(0xFFFFFFFF)
private val L_CardAlt = Color(0xFFF1F5F9)  // slate-100
private val L_CardAlt2 = Color(0xFFE2E8F0) // slate-200
private val L_Text = Color(0xFF0F172A)     // slate-900
private val L_Text2 = Color(0xFF64748B)    // slate-500
private val L_Border = Color(0xFFE2E8F0)   // slate-200
private val L_Outline = Color(0xFF94A3B8)  // slate-400

// Dark neutrals (near-black navy)
private val D_Bg = Color(0xFF020617)      // slate-950
private val D_Card = Color(0xFF0F172A)    // slate-900
private val D_CardAlt = Color(0xFF1E293B)  // slate-800
private val D_CardAlt2 = Color(0xFF334155) // slate-700
private val D_Text = Color(0xFFF1F5F9)
private val D_Text2 = Color(0xFF94A3B8)
private val D_Border = Color(0xFF1E293B)   // slate-800
private val D_Outline = Color(0xFF64748B)  // slate-500

val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF7ED),      // orange-50: pale tile behind orange icons
    onPrimaryContainer = BrandOrangeDeep,
    inversePrimary = Color(0xFFFDBA74),
    secondary = L_Text2,
    onSecondary = Color.White,
    secondaryContainer = L_CardAlt,
    onSecondaryContainer = L_Text,
    tertiary = Color(0xFF2563EB),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBEAFE),
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = L_Bg,
    onBackground = L_Text,
    surface = L_Bg,
    onSurface = L_Text,
    surfaceVariant = L_CardAlt,
    onSurfaceVariant = L_Text2,
    surfaceTint = Color.Transparent,
    inverseSurface = L_Text,
    inverseOnSurface = L_Bg,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = L_Outline,
    outlineVariant = L_Border,
    scrim = Color.Black,
    surfaceBright = L_Card,
    surfaceDim = L_CardAlt2,
    surfaceContainerLowest = L_Card,
    surfaceContainerLow = L_Card,
    surfaceContainer = L_Card,
    surfaceContainerHigh = L_CardAlt,
    surfaceContainerHighest = L_CardAlt2,
)

val DarkColors = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF32241E),      // orange-500 at 15% over slate-900
    onPrimaryContainer = Color(0xFFFB923C),    // orange-400
    inversePrimary = BrandOrangeDeep,
    secondary = D_Text2,
    onSecondary = D_Bg,
    secondaryContainer = D_CardAlt,
    onSecondaryContainer = D_Text,
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B1B3A),
    tertiaryContainer = Color(0xFF1E3A8A),
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = D_Bg,
    onBackground = D_Text,
    surface = D_Bg,
    onSurface = D_Text,
    surfaceVariant = D_CardAlt,
    onSurfaceVariant = D_Text2,
    surfaceTint = Color.Transparent,
    inverseSurface = D_Text,
    inverseOnSurface = D_Bg,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF3B1111),
    onErrorContainer = Color(0xFFFECACA),
    outline = D_Outline,
    outlineVariant = D_Border,
    scrim = Color.Black,
    surfaceBright = D_CardAlt2,
    surfaceDim = D_Bg,
    surfaceContainerLowest = D_Card,
    surfaceContainerLow = D_Card,
    surfaceContainer = D_Card,
    surfaceContainerHigh = D_CardAlt,
    surfaceContainerHighest = D_CardAlt2,
)

/** Semantic accents used in insights / status chips. */
object AppWallColors {
    val success = Color(0xFF16A34A)
    val successContainerLight = Color(0xFFDCFCE7)
    val successOnLight = Color(0xFF166534)
    val successContainerDark = Color(0xFF0F2E1C)
    val successOnDark = Color(0xFF86EFAC)
    val warningContainerLight = Color(0xFFFEF3C7)
    val warningOnLight = Color(0xFF92400E)
    val warningContainerDark = Color(0xFF3A2A08)
    val warningOnDark = Color(0xFFFCD34D)
}
