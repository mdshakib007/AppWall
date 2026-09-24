package io.github.mdshakib007.appwall.ui.blocked

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.R
import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.data.db.FocusSession
import io.github.mdshakib007.appwall.ui.common.AppIcon
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.Format
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.SiteIcon
import io.github.mdshakib007.appwall.ui.theme.LocalIsDark

@Composable
fun BlockedScreen(
    item: BlockItem?,
    host: String?,
    protectedMode: Boolean,
    focus: FocusSession?,
    onGoHome: () -> Unit,
    onOpenAppWall: () -> Unit,
) {
    val now = System.currentTimeMillis()
    val focusActive = BlockRules.isFocusActive(focus, now)
    val dark = LocalIsDark.current
    val bg = Brush.verticalGradient(
        listOf(
            if (dark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.background,
        ),
    )
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else 0.7f, tween(450), label = "pop")

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Image(
                painterResource(R.drawable.logo), contentDescription = null,
                modifier = Modifier.size(140.dp).scale(scale),
            )
            Spacer(Modifier.height(28.dp))
            Text(
                if (protectedMode) "Protected by Focus Mode" else "Blocked by AppWall",
                style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    protectedMode -> "You committed to a focus period. AppWall's settings stay locked until it ends."
                    item?.type == BlockType.WEBSITE -> "This website is on your blocklist. Every subdomain of it is blocked too."
                    else -> "This app is on your blocklist. You chose this, and you can do it."
                },
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            if (item != null) {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.type == BlockType.APP) AppIcon(item.key, 48.dp) else SiteIcon(48.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (item.type == BlockType.WEBSITE && host != null && host != item.key) "You tried: $host" else if (item.type == BlockType.APP) item.key else "All subdomains blocked",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                val reason: Pair<String, PillTone> = when {
                    focusActive -> "Focus Mode · ${Format.focusRemaining(focus!!, now)}" to PillTone.PRIMARY
                    item.untilAt != null -> "Blocked · ${Format.remaining(item.untilAt, now)}" to PillTone.WARNING
                    !item.allDay -> "Scheduled · ${Format.minuteOfDay(item.startMinute)}–${Format.minuteOfDay(item.endMinute)}" to PillTone.WARNING
                    else -> "Blocked permanently" to PillTone.ERROR
                }
                Pill(reason.first, tone = reason.second, icon = if (focusActive) Icons.Rounded.Lock else Icons.Rounded.Schedule)
            } else if (protectedMode && focus != null) {
                Pill("Focus Mode · ${Format.focusRemaining(focus, now)}", tone = PillTone.PRIMARY, icon = Icons.Rounded.Lock)
            }

            Spacer(Modifier.weight(1.2f))
            BigButton("Take me home", onGoHome, icon = Icons.Rounded.Home)
            TextButton(onClick = onOpenAppWall, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) {
                Text("Open AppWall")
            }
        }
    }
}
