@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.mdshakib007.appwall.ui.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.data.db.FocusSession
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.Format
import io.github.mdshakib007.appwall.ui.common.SectionHeader
import io.github.mdshakib007.appwall.ui.common.StatTile
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import io.github.mdshakib007.appwall.ui.onboarding.FocusIllustration
import io.github.mdshakib007.appwall.ui.onboarding.SuccessIllustration
import kotlinx.coroutines.launch

@Composable
fun FocusScreen(bottomPadding: Dp, onAdd: () -> Unit) {
    val state by Graph.engine.stateFlow.collectAsState()
    val history by Graph.repo.focusHistory.collectAsState(initial = emptyList())
    val now = state.now.takeIf { it > 0 } ?: System.currentTimeMillis()
    val active = state.focus?.takeIf { BlockRules.isFocusActive(it, now) }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Focus Mode", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = bottomPadding + 24.dp)) {
            if (active != null) ActiveFocus(active, now, state.items.size, onAdd) else StartFocus(state.items.size, now)

            val past = history.filter { it.endAt <= now }
            if (past.isNotEmpty()) {
                SectionHeader("Past focus periods")
                past.forEach { s ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SuccessIllustration(Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${s.days} days completed", style = MaterialTheme.typography.titleSmall)
                            Text("${Format.date(s.startAt)} – ${Format.date(s.endAt)} · ${s.itemsAtStart} blocks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartFocus(itemCount: Int, now: Long) {
    val scope = rememberCoroutineScope()
    var days by remember { mutableIntStateOf(21) }
    var confirm by remember { mutableStateOf(false) }
    var understood by remember { mutableStateOf(false) }
    val state by Graph.engine.stateFlow.collectAsState()
    val apps = state.items.count { it.type == BlockType.APP }
    val sites = state.items.count { it.type == BlockType.WEBSITE }

    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FocusIllustration(Modifier.size(170.dp).padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        Text("Commit for a while", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Focus Mode locks your entire blocklist, all day, for as many days as you choose. You can add more blocks, but nothing can be edited or removed until it ends. There is no way to cancel it.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        Text("$days", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Text(if (days == 1) "day" else "days", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = days.toFloat(), onValueChange = { days = it.toInt().coerceIn(1, 365) },
            valueRange = 1f..365f, modifier = Modifier.padding(vertical = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 7, 14, 21, 30, 60, 90, 180, 365).forEach { d ->
                val sel = days == d
                Box(
                    Modifier.background(if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .clickable { days = d }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("${d}d", style = MaterialTheme.typography.labelLarge, color = if (sel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        SurfaceCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("What gets locked", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (itemCount == 0) "Your blocklist is empty. Add at least one app or website first."
                    else "$apps app${if (apps == 1) "" else "s"} and $sites website${if (sites == 1) "" else "s"}, blocked around the clock until ${Format.dateTime(now + days * 24L * 3600_000)}.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        BigButton("Start Focus Mode", onClick = { understood = false; confirm = true }, enabled = itemCount > 0, icon = Icons.Rounded.Lock)
        Spacer(Modifier.height(8.dp))
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Lock everything for $days days?") },
            text = {
                Column {
                    Text("Until ${Format.dateTime(now + days * 24L * 3600_000)} you will not be able to unblock, edit, or remove anything. Not even by reinstalling settings. This is a one-way door.")
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { understood = !understood }) {
                        Checkbox(checked = understood, onCheckedChange = { understood = it })
                        Text("I understand this cannot be undone", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = understood, onClick = {
                    confirm = false
                    scope.launch { runCatching { Graph.repo.startFocus(days) } }
                }) { Text("Lock it") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Not now") } },
        )
    }
}

@Composable
private fun ActiveFocus(session: FocusSession, now: Long, itemCount: Int, onAdd: () -> Unit) {
    val progress = ((now - session.startAt).toFloat() / (session.endAt - session.startAt)).coerceIn(0f, 1f)
    val msLeft = (session.endAt - now).coerceAtLeast(0)
    val daysLeft = ((msLeft + 24L * 3600_000 - 1) / (24L * 3600_000)).toInt() // rounds up: a fresh 3-day lock reads "3 days"
    val hoursLeft = ((msLeft % (24L * 3600_000)) / 3600_000).toInt()
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(220.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 18.dp.toPx()
                val inset = stroke / 2
                drawArc(track, -90f, 360f, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(primary, -90f, 360f * progress, false, Offset(inset, inset), Size(size.width - stroke, size.height - stroke), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (msLeft >= 24L * 3600_000) {
                    Text("$daysLeft", style = MaterialTheme.typography.displayLarge, color = primary)
                    Text(if (daysLeft == 1) "day left" else "days left", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("$hoursLeft", style = MaterialTheme.typography.displayLarge, color = primary)
                    Text(if (hoursLeft == 1) "hour left" else "hours left", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("You're in Focus Mode", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Ends ${Format.dateTime(session.endAt)}", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("$itemCount", "blocks locked", Modifier.weight(1f))
            StatTile("${session.days}", "days committed", Modifier.weight(1f), accent = true)
        }
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(8.dp)); Text("Add more blocks")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Tip: AppWall keeps you out of its own Settings pages while Focus Mode is on, so there's no quick escape hatch.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
    }
}
