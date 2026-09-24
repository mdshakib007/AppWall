@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.mdshakib007.appwall.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import io.github.mdshakib007.appwall.ui.Emphasized
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.Savings
import io.github.mdshakib007.appwall.data.AppUsage
import io.github.mdshakib007.appwall.ui.common.AppTopBar
import io.github.mdshakib007.appwall.ui.common.AppIcon
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.Format
import io.github.mdshakib007.appwall.ui.common.IconTile
import io.github.mdshakib007.appwall.ui.common.Segmented
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.ui.unit.sp
import io.github.mdshakib007.appwall.ui.common.Permissions
import io.github.mdshakib007.appwall.ui.common.SectionHeader
import io.github.mdshakib007.appwall.ui.common.SiteIcon
import io.github.mdshakib007.appwall.ui.common.StatTile
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import io.github.mdshakib007.appwall.ui.common.rememberPermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private class UsageSnapshot(
    val todayMs: Long,
    val yesterdayMs: Long,
    val daily: List<Pair<LocalDate, Long>>,
    val topToday: List<AppUsage>,
    val topWeek: List<AppUsage>,
)

@Composable
fun InsightsScreen(bottomPadding: Dp) {
    val context = LocalContext.current
    val perms by rememberPermissionStatus()
    val state by Graph.engine.stateFlow.collectAsState()
    val now = System.currentTimeMillis()
    val startOfDay = Graph.usage.startOfDay(now)
    val weekAgo = startOfDay - 6 * 24L * 3600_000
    val attemptsToday by Graph.repo.attemptCountSince(startOfDay).collectAsState(initial = 0)
    val attemptsWeek by Graph.repo.attemptCountSince(weekAgo).collectAsState(initial = 0)
    val attemptsAll by Graph.repo.totalAttempts().collectAsState(initial = 0)
    val allAttempts by Graph.repo.attemptsSince(0).collectAsState(initial = emptyList())
    val siteSessions by Graph.repo.siteSessionsSince(weekAgo).collectAsState(initial = emptyList())
    var range by remember { mutableIntStateOf(0) } // 0 today, 1 week
    var explain by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<UsageSnapshot?>(null) }

    LaunchedEffect(perms.usage, state.items.size) {
        if (perms.usage) snapshot = withContext(Dispatchers.IO) {
            val u = Graph.usage
            val daily = u.dailyTotals(7, now)
            UsageSnapshot(
                todayMs = daily.lastOrNull()?.second ?: 0L,
                yesterdayMs = daily.getOrNull(daily.size - 2)?.second ?: 0L,
                daily = daily,
                topToday = u.topApps(startOfDay, now, 8),
                topWeek = u.topApps(weekAgo, now, 8),
            )
        }
    }

    val attemptsByKey = allAttempts.groupingBy { it.type.name + ":" + it.key }.eachCount()
    val saved = Savings.total(state.items, attemptsByKey, now)

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = { Text("Insights", fontWeight = FontWeight.Bold) },
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = bottomPadding + 24.dp)) {
            // Time saved hero
            SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Schedule, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("TIME YOU GOT BACK", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { explain = true }, Modifier.size(24.dp)) { Icon(Icons.Rounded.Info, "How is this calculated?", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(Format.duration(saved, compact = true), style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        "since you started blocking · $attemptsAll attempts stopped",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("$attemptsToday", "blocked today", Modifier.weight(1f))
                StatTile("$attemptsWeek", "blocked this week", Modifier.weight(1f))
            }

            if (!perms.usage) {
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("See your real screen time", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Allow Usage access and AppWall will show which apps eat your day. It reads only durations, never content, and it stays on this phone.",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(10.dp))
                        BigButton("Allow usage access", { context.startActivity(Permissions.usageIntent()) })
                    }
                }
            } else snapshot?.let { s ->
                // Screen time today
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("SCREEN TIME TODAY", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(Format.duration(s.todayMs, compact = true), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        val diff = s.todayMs - s.yesterdayMs
                        if (s.yesterdayMs > 0) Text(
                            (if (diff <= 0) "▼ " else "▲ ") + Format.duration(diff, compact = true) + " vs yesterday at this point of the week",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (diff <= 0) io.github.mdshakib007.appwall.ui.theme.AppWallColors.success else MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(16.dp))
                        WeekChart(s.daily, Modifier.fillMaxWidth().height(120.dp))
                    }
                }

                SectionHeader(
                    "Top apps",
                    trailing = { Segmented(listOf("Today", "7 days"), range, { range = it }, Modifier.width(160.dp)) },
                )
                val list = if (range == 0) s.topToday else s.topWeek
                val max = list.maxOfOrNull { it.totalMs } ?: 1L
                if (list.isEmpty()) Text("No usage recorded yet.", Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                list.forEach { a ->
                    val blocked = a.packageName in state.itemByPackage
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(a.packageName, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(Graph.installedApps.label(a.packageName), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(Format.duration(a.totalMs, compact = true), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { a.totalMs.toFloat() / max }, modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = if (blocked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            // Websites (from address-bar tracking)
            val byDomain = siteSessions.filter { if (range == 0) it.endAt >= startOfDay else true }
                .groupBy { it.domain }.mapValues { e -> e.value.sumOf { it.endAt - it.startAt } }
                .entries.sortedByDescending { it.value }.take(8)
            if (byDomain.isNotEmpty()) {
                SectionHeader("Websites")
                val max = byDomain.maxOf { it.value }.coerceAtLeast(1)
                byDomain.forEach { (domain, ms) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        SiteIcon(40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                Text(domain, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(Format.duration(ms, compact = true), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(progress = { ms.toFloat() / max }, modifier = Modifier.fillMaxWidth().height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHigh, strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                }
                Text(
                    "Website time is measured while a site is open in a browser. It's only tracked when AppWall's accessibility service is on.",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text("How is this calculated?") },
            text = {
                Text(
                    "For each blocked app, AppWall looks at how much you used it in the 7 days before you blocked it, and credits that daily average for every day it has been blocked (scaled down if the block only covers part of the day).\n\n" +
                        "If there's no usage history (for example a website, or Usage access was off), it credits a conservative 4 minutes per blocked attempt instead.\n\n" +
                        "It's an estimate, computed entirely on your phone, meant to keep you motivated rather than to be exact.",
                )
            },
            confirmButton = { TextButton(onClick = { explain = false }) { Text("Got it") } },
        )
    }
}

@Composable
private fun WeekChart(daily: List<Pair<LocalDate, Long>>, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val max = (daily.maxOfOrNull { it.second } ?: 1L).coerceAtLeast(1L)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(daily) { shown = true }
    val grow by animateFloatAsState(if (shown) 1f else 0f, tween(800, easing = Emphasized), label = "bars")
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val n = daily.size.coerceAtLeast(1)
            val slot = size.width / n
            val barW = slot * 0.55f
            daily.forEachIndexed { i, (_, ms) ->
                val h = (ms.toFloat() / max) * size.height * grow
                val x = i * slot + (slot - barW) / 2
                drawRoundRect(track, Offset(x, 0f), Size(barW, size.height), CornerRadius(barW / 2))
                if (h > 0) drawRoundRect(if (i == n - 1) primary else primary.copy(alpha = 0.55f), Offset(x, size.height - h), Size(barW, h), CornerRadius(barW / 2))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            daily.forEach { (d, _) ->
                Text(
                    d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall,
                    color = labelColor, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
