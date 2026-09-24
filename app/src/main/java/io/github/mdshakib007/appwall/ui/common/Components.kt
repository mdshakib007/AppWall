package io.github.mdshakib007.appwall.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.ui.theme.AppWallColors
import io.github.mdshakib007.appwall.ui.theme.LocalIsDark

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick, modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = MaterialTheme.shapes.large, content = content,
        )
    } else {
        Card(
            modifier = modifier, colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = MaterialTheme.shapes.large, content = content,
        )
    }
}

@Composable
fun BigButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        if (icon != null) { Icon(icon, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatusDot(active: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.size(8.dp).background(
            if (active) AppWallColors.success else MaterialTheme.colorScheme.outlineVariant, CircleShape,
        )
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, tone: PillTone = PillTone.NEUTRAL, icon: ImageVector? = null) {
    val dark = LocalIsDark.current
    val (bg, fg) = when (tone) {
        PillTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
        PillTone.PRIMARY -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        PillTone.SUCCESS -> (if (dark) AppWallColors.successContainerDark else AppWallColors.successContainerLight) to (if (dark) Color(0xFF9BE3B7) else Color(0xFF14532D))
        PillTone.WARNING -> (if (dark) Color(0xFF4A3300) else Color(0xFFFFF0C2)) to (if (dark) Color(0xFFFFD98A) else Color(0xFF5C3D00))
        PillTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Row(
        modifier.background(bg, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = fg)
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

enum class PillTone { NEUTRAL, PRIMARY, SUCCESS, WARNING, ERROR }

@Composable
fun StatTile(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    SurfaceCard(
        modifier,
        containerColor = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                value, style = MaterialTheme.typography.headlineSmall,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                label, style = MaterialTheme.typography.bodySmall,
                color = if (accent) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RowScope.Gap(width: Int = 12) = Spacer(Modifier.width(width.dp))

@Composable
fun ColumnScope.VGap(height: Int = 12) = Spacer(Modifier.height(height.dp))
