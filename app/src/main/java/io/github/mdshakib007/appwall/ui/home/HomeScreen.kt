@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.mdshakib007.appwall.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.R
import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.core.Savings
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.ui.common.AppIcon
import io.github.mdshakib007.appwall.ui.common.Format
import io.github.mdshakib007.appwall.ui.common.IconTile
import io.github.mdshakib007.appwall.ui.common.SmallButton
import io.github.mdshakib007.appwall.ui.common.Permissions
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.SectionHeader
import io.github.mdshakib007.appwall.ui.common.SiteIcon
import io.github.mdshakib007.appwall.ui.common.StatTile
import io.github.mdshakib007.appwall.ui.common.StatusDot
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import io.github.mdshakib007.appwall.ui.common.rememberPermissionStatus
import io.github.mdshakib007.appwall.ui.onboarding.EmptyIllustration

@Composable
fun HomeScreen(
    bottomPadding: Dp,
    onAdd: (tab: Int) -> Unit,
    onOpenItem: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFocus: () -> Unit,
) {
    val context = LocalContext.current
    val state by Graph.engine.stateFlow.collectAsState()
    val perms by rememberPermissionStatus()
    val now = state.now.takeIf { it > 0 } ?: System.currentTimeMillis()
    val startOfDay = Graph.usage.startOfDay(now)
    val attemptsToday by Graph.repo.attemptCountSince(startOfDay).collectAsState(initial = 0)
    val allAttempts by Graph.repo.attemptsSince(0).collectAsState(initial = emptyList())
    val attemptsByKey = allAttempts.groupingBy { it.type.name + ":" + it.key }.eachCount()
    val saved = Savings.total(state.items, attemptsByKey, now)

    val apps = state.items.filter { it.type == BlockType.APP }
    val sites = state.items.filter { it.type == BlockType.WEBSITE }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.logo), null, Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("AppWall", fontWeight = FontWeight.Bold)
                    }
                },
                actions = { IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(0) },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Block something") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        },
    ) { inner ->
        LazyColumn(
            Modifier.fillMaxSize().padding(top = inner.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = bottomPadding + 96.dp),
        ) {
            // Protection status
            item {
                if (!perms.coreReady) {
                    WarningCard(
                        title = "Protection is off",
                        body = if (!perms.accessibility) "Turn on the AppWall accessibility service so blocking can work."
                        else "Allow AppWall to display over other apps to show the Blocked screen.",
                        action = "Fix now",
                        onClick = {
                            context.startActivity(if (!perms.accessibility) Permissions.accessibilityIntent() else Permissions.overlayIntent(context))
                        },
                    )
                } else if (sites.isNotEmpty() && perms.privateDnsStrict) {
                    WarningCard(
                        title = "Private DNS is set to a custom provider",
                        body = "Android will send website lookups straight to that provider, bypassing AppWall's filter in some apps. Set Private DNS to Automatic or Off.",
                        action = "Open settings",
                        onClick = { context.startActivity(Permissions.privateDnsIntent()) },
                    )
                } else if (sites.isNotEmpty() && !perms.vpnRunning) {
                    WarningCard(
                        title = "Website filter is not running",
                        body = "Sites are still blocked in your browsers, but in-app browsers may slip through. Turn the filter back on in Settings.",
                        action = "Settings",
                        onClick = onOpenSettings,
                    )
                }
            }

            // Focus banner
            state.focus?.takeIf { BlockRules.isFocusActive(it, now) }?.let { focus ->
                item {
                    SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(), onClick = onOpenFocus) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconTile(Icons.Rounded.Lock, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Focus Mode", style = MaterialTheme.typography.titleMedium)
                                    Text("Locked until ${Format.dateTime(focus.endAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Pill(Format.focusRemaining(focus, now), tone = PillTone.PRIMARY)
                            }
                            Spacer(Modifier.height(12.dp))
                            val progress = ((now - focus.startAt).toFloat() / (focus.endAt - focus.startAt)).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp),
                                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                            )
                        }
                    }
                }
            }

            // Stats strip
            item {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("$attemptsToday", "blocked today", Modifier.weight(1f), icon = Icons.Rounded.Shield)
                    StatTile(Format.duration(saved, compact = true), "time you got back", Modifier.weight(1f), accent = true, icon = Icons.Rounded.Schedule)
                }
            }

            if (state.items.isEmpty()) {
                item { EmptyState(onAdd) }
            }

            if (apps.isNotEmpty()) {
                item { SectionHeader("Apps · ${apps.size}") }
                items(apps, key = { "a" + it.id }) { BlockRow(it, state.focusActive, now, onOpenItem, Modifier.animateItem()) }
            }
            if (sites.isNotEmpty()) {
                item { SectionHeader("Websites · ${sites.size}") }
                items(sites, key = { "s" + it.id }) { BlockRow(it, state.focusActive, now, onOpenItem, Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun BlockRow(item: BlockItem, focusActive: Boolean, now: Long, onOpen: (Long) -> Unit, modifier: Modifier = Modifier) {
    val active = focusActive || BlockRules.isActive(item, now)
    val ended = !focusActive && BlockRules.hasEnded(item, now)
    Row(
        modifier.fillMaxWidth().clickable { onOpen(item.id) }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.type == BlockType.APP) AppIcon(item.key, 46.dp) else SiteIcon(46.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(item.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(active)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (focusActive) "Locked by Focus Mode" else if (ended) "Ended · tap to extend" else Format.schedule(item, now),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (ended) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (focusActive) Icon(Icons.Rounded.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun WarningCard(title: String, body: String, action: String, onClick: () -> Unit) {
    SurfaceCard(
        Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer, onClick = onClick,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f))
            }
            Spacer(Modifier.width(8.dp))
            SmallButton(action, onClick)
        }
    }
}

@Composable
private fun EmptyState(onAdd: (Int) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EmptyIllustration(Modifier.size(180.dp))
        Spacer(Modifier.height(20.dp))
        Text("Nothing blocked yet", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick a few time-eaters below to get started. You can block any app or any website, with any schedule.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickChoice("Websites", Icons.Rounded.Language) { onAdd(0) }
            QuickChoice("Apps", Icons.Rounded.Apps) { onAdd(1) }
        }
    }
}

@Composable
private fun QuickChoice(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
