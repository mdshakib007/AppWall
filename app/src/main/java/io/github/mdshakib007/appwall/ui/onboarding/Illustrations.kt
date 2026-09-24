package io.github.mdshakib007.appwall.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.mdshakib007.appwall.ui.theme.AppWallColors
import io.github.mdshakib007.appwall.ui.theme.LocalIsDark

/**
 * Hand-drawn vector illustrations. Zero image assets, themed automatically, weigh nothing in the APK.
 * Each one is drawn in a 200x200 unit space and scales with the given modifier.
 */

private fun DrawScope.unit(): Float = minOf(size.width, size.height) / 200f

private fun DrawScope.blob(color: Color, cx: Float, cy: Float, r: Float) {
    val u = unit()
    drawCircle(color, r * u, Offset(cx * u, cy * u))
}

private fun DrawScope.shield(cx: Float, cy: Float, s: Float, fill: Color, inner: Color, slash: Color, blockedGlyphs: Boolean) {
    val u = unit()
    val path = Path().apply {
        moveTo((cx) * u, (cy - s) * u)
        lineTo((cx + s * 0.9f) * u, (cy - s * 0.65f) * u)
        lineTo((cx + s * 0.9f) * u, (cy + s * 0.05f) * u)
        cubicTo((cx + s * 0.9f) * u, (cy + s * 0.65f) * u, (cx + s * 0.45f) * u, (cy + s * 0.95f) * u, cx * u, (cy + s * 1.1f) * u)
        cubicTo((cx - s * 0.45f) * u, (cy + s * 0.95f) * u, (cx - s * 0.9f) * u, (cy + s * 0.65f) * u, (cx - s * 0.9f) * u, (cy + s * 0.05f) * u)
        lineTo((cx - s * 0.9f) * u, (cy - s * 0.65f) * u)
        close()
    }
    drawPath(path, fill)
    drawCircle(inner, s * 0.52f * u, Offset(cx * u, (cy + s * 0.05f) * u))
    if (blockedGlyphs) {
        val g = s * 0.22f
        drawRoundRect(slash, Offset((cx - s * 0.42f) * u, (cy - s * 0.32f) * u), Size(g * 1.4f * u, g * 1.4f * u), CornerRadius(g * 0.35f * u))
        drawCircle(slash, g * 0.75f * u, Offset((cx + s * 0.22f) * u, (cy + s * 0.32f) * u), style = Stroke(g * 0.22f * u))
        drawLine(fill, Offset((cx - s * 0.4f) * u, (cy + s * 0.45f) * u), Offset((cx + s * 0.4f) * u, (cy - s * 0.35f) * u), strokeWidth = s * 0.13f * u, cap = StrokeCap.Round)
    }
}

private fun DrawScope.phone(cx: Float, cy: Float, w: Float, h: Float, body: Color, screen: Color) {
    val u = unit()
    drawRoundRect(body, Offset((cx - w / 2) * u, (cy - h / 2) * u), Size(w * u, h * u), CornerRadius(14 * u))
    drawRoundRect(screen, Offset((cx - w / 2 + 5) * u, (cy - h / 2 + 8) * u), Size((w - 10) * u, (h - 16) * u), CornerRadius(10 * u))
    drawRoundRect(body, Offset((cx - 10) * u, (cy - h / 2 + 12) * u), Size(20 * u, 3 * u), CornerRadius(2 * u))
}

@Composable
fun BlockIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val dark = MaterialTheme.colorScheme.onSurface
    val line = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val u = unit()
        blob(container.copy(alpha = 0.6f), 100f, 100f, 90f)
        phone(100f, 105f, 96f, 170f, dark, surface)
        // app grid rows
        for (r in 0 until 3) for (c in 0 until 3) {
            val x = 70f + c * 30f; val y = 60f + r * 30f
            drawRoundRect(if ((r + c) % 2 == 0) line else container, Offset((x - 9) * u, (y - 9) * u), Size(18 * u, 18 * u), CornerRadius(5 * u))
        }
        shield(100f, 112f, 34f, primary, surface, dark, true)
    }
}

@Composable
fun PrivacyIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val dark = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val u = unit()
        blob(container.copy(alpha = 0.6f), 100f, 100f, 90f)
        phone(80f, 110f, 90f, 160f, dark, surface)
        // padlock on the phone
        drawRoundRect(primary, Offset(58f * u, 108f * u), Size(44 * u, 34 * u), CornerRadius(8 * u))
        drawArc(primary, 180f, 180f, false, Offset(66f * u, 84f * u), Size(28 * u, 40 * u), style = Stroke(7 * u, cap = StrokeCap.Round))
        drawCircle(surface, 5 * u, Offset(80f * u, 122f * u))
        drawRoundRect(surface, Offset(78f * u, 122f * u), Size(4 * u, 10 * u), CornerRadius(2 * u))
        // crossed-out cloud
        val cx = 152f; val cy = 60f
        drawCircle(muted, 14 * u, Offset((cx - 10) * u, cy * u))
        drawCircle(muted, 18 * u, Offset((cx + 4) * u, (cy - 6) * u))
        drawCircle(muted, 13 * u, Offset((cx + 18) * u, (cy + 2) * u))
        drawRoundRect(muted, Offset((cx - 22) * u, (cy - 2) * u), Size(52 * u, 16 * u), CornerRadius(8 * u))
        drawLine(primary, Offset((cx - 26) * u, (cy + 22) * u), Offset((cx + 30) * u, (cy - 26) * u), 6 * u, StrokeCap.Round)
        // dashed "no signal" between phone and cloud
        for (i in 0 until 4) drawCircle(muted.copy(alpha = 0.5f), 2.5f * u, Offset((118f + i * 8) * u, (92f - i * 6) * u))
    }
}

@Composable
fun OpenSourceIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val dark = MaterialTheme.colorScheme.onSurface
    val line = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val u = unit()
        blob(container.copy(alpha = 0.6f), 100f, 100f, 90f)
        // document
        drawRoundRect(surface, Offset(50f * u, 40f * u), Size(100 * u, 125 * u), CornerRadius(12 * u))
        drawRoundRect(dark, Offset(50f * u, 40f * u), Size(100 * u, 125 * u), CornerRadius(12 * u), style = Stroke(3 * u))
        val widths = listOf(48f, 64f, 36f, 58f, 44f, 62f)
        widths.forEachIndexed { i, w ->
            val y = 62f + i * 15f
            val indent = if (i % 3 == 0) 0f else 12f
            drawRoundRect(if (i % 2 == 0) primary else line, Offset((64f + indent) * u, y * u), Size(w * u, 6 * u), CornerRadius(3 * u))
        }
        // angle brackets
        val stroke = Stroke(6 * u, cap = StrokeCap.Round)
        val left = Path().apply { moveTo(44f * u, 96f * u); lineTo(28f * u, 112f * u); lineTo(44f * u, 128f * u) }
        val right = Path().apply { moveTo(156f * u, 96f * u); lineTo(172f * u, 112f * u); lineTo(156f * u, 128f * u) }
        drawPath(left, primary, style = stroke); drawPath(right, primary, style = stroke)
        // heart
        val hx = 132f; val hy = 150f
        drawCircle(primary, 8 * u, Offset((hx - 6) * u, (hy - 4) * u)); drawCircle(primary, 8 * u, Offset((hx + 6) * u, (hy - 4) * u))
        val heart = Path().apply { moveTo((hx - 13) * u, (hy - 1) * u); lineTo(hx * u, (hy + 14) * u); lineTo((hx + 13) * u, (hy - 1) * u); close() }
        drawPath(heart, primary)
    }
}

@Composable
fun FocusIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    val dark = MaterialTheme.colorScheme.onSurface
    Canvas(modifier) {
        val u = unit()
        blob(container.copy(alpha = 0.6f), 100f, 100f, 90f)
        drawCircle(surface, 62 * u, Offset(100f * u, 100f * u))
        drawCircle(primary, 62 * u, Offset(100f * u, 100f * u), style = Stroke(10 * u))
        drawCircle(primary, 38 * u, Offset(100f * u, 100f * u), style = Stroke(10 * u))
        drawCircle(primary, 14 * u, Offset(100f * u, 100f * u))
        // arrow
        rotate(-35f, Offset(100f * u, 100f * u)) {
            drawLine(dark, Offset(100f * u, 100f * u), Offset(100f * u, 22f * u), 6 * u, StrokeCap.Round)
            val fin = Path().apply { moveTo(100f * u, 22f * u); lineTo(112f * u, 40f * u); lineTo(100f * u, 34f * u); lineTo(88f * u, 40f * u); close() }
            drawPath(fin, dark)
        }
    }
}

@Composable
fun EmptyIllustration(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    Canvas(modifier) {
        val u = unit()
        blob(container.copy(alpha = 0.5f), 100f, 100f, 90f)
        shield(100f, 95f, 55f, primary, surface, primary, false)
        drawRoundRect(primary, Offset(96f * u, 78f * u), Size(8 * u, 44 * u), CornerRadius(4 * u))
        drawRoundRect(primary, Offset(78f * u, 96f * u), Size(44 * u, 8 * u), CornerRadius(4 * u))
    }
}

@Composable
fun SuccessIllustration(modifier: Modifier = Modifier) {
    val success = AppWallColors.success
    val container = if (LocalIsDark.current) AppWallColors.successContainerDark else AppWallColors.successContainerLight
    Canvas(modifier) {
        val u = unit()
        blob(container, 100f, 100f, 90f)
        drawCircle(success, 52 * u, Offset(100f * u, 100f * u))
        val check = Path().apply { moveTo(74f * u, 102f * u); lineTo(92f * u, 120f * u); lineTo(128f * u, 82f * u) }
        drawPath(check, Color.White, style = Stroke(10 * u, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
