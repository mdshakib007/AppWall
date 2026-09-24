package io.github.mdshakib007.appwall.ui.blocked

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
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
    val glow = Brush.radialGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), androidx.compose.ui.graphics.Color.Transparent))
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else 0.7f, tween(450), label = "pop")

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(200.dp).background(glow, CircleShape), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.logo), contentDescription = null, modifier = Modifier.size(120.dp).scale(scale))
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (protectedMode) "Protected by Focus Mode" else "Blocked by AppWall",
                style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    protectedMode -> "You committed to a focus period. AppWall's settings stay locked until it ends."
                    item?.type == BlockType.WEBSITE -> "This website is on your blocklist, including all of its subdomains."
                    else -> "This app is on your blocklist. You chose this, and you can do it."
                },
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            if (item != null) {
                Column(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (item.type == BlockType.APP) AppIcon(item.key, 64.dp) else SiteIcon(64.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        item.displayName, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    val reason: Pair<String, PillTone> = when {
                        focusActive -> "Focus Mode · ${Format.focusRemaining(focus!!, now)}" to PillTone.PRIMARY
                        item.untilAt != null -> "Blocked · ${Format.remaining(item.untilAt, now)}" to PillTone.WARNING
                        !item.allDay -> "Scheduled · ${Format.minuteOfDay(item.startMinute)}–${Format.minuteOfDay(item.endMinute)}" to PillTone.WARNING
                        else -> "Blocked permanently" to PillTone.ERROR
                    }
                    Pill(reason.first, tone = reason.second, icon = if (focusActive) Icons.Rounded.Lock else Icons.Rounded.Schedule)
                }
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
