package io.github.mdshakib007.appwall.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Brand orange used across the app (matches the logo). */
val BrandOrange = Color(0xFFF26A1B)
val BrandOrangeDeep = Color(0xFFE4520A)

val LightColors = lightColorScheme(
    primary = Color(0xFFE85F0C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC9),
    onPrimaryContainer = Color(0xFF331200),
    inversePrimary = Color(0xFFFFB68F),
    secondary = Color(0xFF765749),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC9),
    onSecondaryContainer = Color(0xFF2B160B),
    tertiary = Color(0xFF665F31),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEE3A9),
    onTertiaryContainer = Color(0xFF201C00),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443C),
    surfaceTint = Color(0xFFE85F0C),
    inverseSurface = Color(0xFF372F2B),
    inverseOnSurface = Color(0xFFFEEEE8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF85736B),
    outlineVariant = Color(0xFFD7C2B9),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F6),
    surfaceDim = Color(0xFFE7D7D1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1EB),
    surfaceContainer = Color(0xFFFBEBE4),
    surfaceContainerHigh = Color(0xFFF6E5DE),
    surfaceContainerHighest = Color(0xFFF0E0D9),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A3D),
    onPrimary = Color(0xFF3D1500),
    primaryContainer = Color(0xFF6E2B00),
    onPrimaryContainer = Color(0xFFFFDBC9),
    inversePrimary = Color(0xFFE85F0C),
    secondary = Color(0xFFE6BEAC),
    onSecondary = Color(0xFF432A1E),
    secondaryContainer = Color(0xFF5C4033),
    onSecondaryContainer = Color(0xFFFFDBC9),
    tertiary = Color(0xFFD1C78F),
    onTertiary = Color(0xFF363107),
    tertiaryContainer = Color(0xFF4D481C),
    onTertiaryContainer = Color(0xFFEEE3A9),
    background = Color(0xFF141110),
    onBackground = Color(0xFFF0DFD9),
    surface = Color(0xFF141110),
    onSurface = Color(0xFFF0DFD9),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C2B9),
    surfaceTint = Color(0xFFFF8A3D),
    inverseSurface = Color(0xFFF0DFD9),
    inverseOnSurface = Color(0xFF372F2B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFFA08D84),
    outlineVariant = Color(0xFF52443C),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3C3735),
    surfaceDim = Color(0xFF141110),
    surfaceContainerLowest = Color(0xFF0F0B0A),
    surfaceContainerLow = Color(0xFF1D1917),
    surfaceContainer = Color(0xFF221D1B),
    surfaceContainerHigh = Color(0xFF2D2725),
    surfaceContainerHighest = Color(0xFF383230),
)

/** Semantic accents used in insights / status chips. */
object AppWallColors {
    val success = Color(0xFF2E9E5B)
    val successContainerLight = Color(0xFFD9F5E3)
    val successContainerDark = Color(0xFF14432A)
    val warning = Color(0xFFE9A500)
    val info = Color(0xFF3B82F6)
}
