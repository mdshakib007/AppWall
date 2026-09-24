@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.mdshakib007.appwall.ui.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.BuildConfig
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.R
import io.github.mdshakib007.appwall.service.DnsFilterVpnService
import io.github.mdshakib007.appwall.ui.common.AppTopBar
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.Permissions
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.SectionHeader
import io.github.mdshakib007.appwall.ui.common.Segmented
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import io.github.mdshakib007.appwall.ui.common.rememberPermissionStatus
import io.github.mdshakib007.appwall.ui.onboarding.GITHUB_URL
import io.github.mdshakib007.appwall.ui.onboarding.PrivacyIllustration
import io.github.mdshakib007.appwall.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit, onAbout: () -> Unit, onPrivacy: () -> Unit, onOnboarding: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val perms by rememberPermissionStatus()
    val theme by Graph.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val dnsWanted by Graph.prefs.dnsFilterEnabled.collectAsState(initial = true)
    val guard by Graph.prefs.settingsGuard.collectAsState(initial = true)
    val vpnRunning by DnsFilterVpnService.running.collectAsState()
    val revoked by DnsFilterVpnService.revoked.collectAsState()
    val state by Graph.engine.stateFlow.collectAsState()
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) { scope.launch { Graph.prefs.setDnsFilterEnabled(true) }; DnsFilterVpnService.start(context) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            SectionHeader("Appearance")
            Segmented(
                listOf("Light", "Dark", "System"),
                selected = when (theme) { ThemeMode.LIGHT -> 0; ThemeMode.DARK -> 1; ThemeMode.SYSTEM -> 2 },
                onSelect = { i -> scope.launch { Graph.prefs.setThemeMode(listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM)[i]) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            SectionHeader("Protection")
            PermRow("Accessibility service", "Required for all blocking", perms.accessibility) { context.startActivity(Permissions.accessibilityIntent()) }
            PermRow("Display over other apps", "Shows the Blocked screen", perms.overlay) { context.startActivity(Permissions.overlayIntent(context)) }
            PermRow("Usage access", "Screen-time insights", perms.usage) { context.startActivity(Permissions.usageIntent()) }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Strict website blocking", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when {
                            state.focusActive && dnsWanted -> "Locked on by Focus Mode"
                            revoked -> "Stopped: another VPN took over. Turn it back on to re-enable."
                            vpnRunning -> "On · a local DNS filter also blocks sites for apps themselves. Uses Android's VPN slot; no traffic leaves through it."
                            dnsWanted -> "Starting…"
                            else -> "Off · sites are blocked in browsers and in-app browsers. Turn on to also block them at the network level (uses Android's VPN slot)."
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = dnsWanted && vpnRunning,
                    enabled = !(state.focusActive && dnsWanted),
                    onCheckedChange = { on ->
                        if (on) {
                            if (android.os.Build.VERSION.SDK_INT >= 33 && !perms.notifications) notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            val consent = Permissions.vpnConsentIntent(context)
                            if (consent != null) vpnLauncher.launch(consent) else { scope.launch { Graph.prefs.setDnsFilterEnabled(true) }; DnsFilterVpnService.start(context) }
                        } else {
                            scope.launch { Graph.prefs.setDnsFilterEnabled(false) }
                            DnsFilterVpnService.stop(context)
                        }
                    },
                )
            }
            if (perms.privateDnsStrict) {
                SurfaceCard(Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(), containerColor = MaterialTheme.colorScheme.errorContainer, onClick = { context.startActivity(Permissions.privateDnsIntent()) }) {
                    Text(
                        "Private DNS is set to a custom provider, which can bypass the website filter in some apps. Set it to Automatic or Off in Network settings.",
                        Modifier.padding(14.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            SectionHeader("Focus Mode")
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Guard AppWall's settings pages", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "While Focus Mode is on, AppWall closes system pages that could disable it (accessibility, app info, VPN).",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = guard, enabled = !state.focusActive, onCheckedChange = { scope.launch { Graph.prefs.setSettingsGuard(it) } })
            }

            SectionHeader("About")
            LinkRow("How AppWall protects your privacy", onPrivacy)
            LinkRow("About AppWall · v${BuildConfig.VERSION_NAME}", onAbout)
            LinkRow("Source code on GitHub", { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }, external = true)
            LinkRow("Replay the intro", onOnboarding)
        }
    }
}

@Composable
private fun PermRow(title: String, subtitle: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Pill(if (granted) "On" else "Off", tone = if (granted) PillTone.SUCCESS else PillTone.ERROR)
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit, external: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(if (external) Icons.Rounded.OpenInNew else Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit, onPrivacy: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = { Text("About", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { inner ->
        Column(
            Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(120.dp))
            Spacer(Modifier.height(12.dp))
            Text("AppWall", style = MaterialTheme.typography.headlineLarge)
            Text("Version ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Text(
                "A free, open-source app and website blocker. No server, no account, no analytics. Built to be verified, not to be taken on faith.",
                style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            BigButton("Source code on GitHub", { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }, icon = Icons.Rounded.OpenInNew)
            Spacer(Modifier.height(10.dp))
            FilledTonalButton(
                onClick = onPrivacy, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.medium,
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
            ) { Text("How your privacy is protected", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(28.dp))
            Text("Contribute", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Found a bug, want a new browser supported, or have a better idea? Open an issue or a pull request. Every improvement helps everyone who uses this app.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))
            Text("Built with", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Kotlin · Jetpack Compose · Material 3 · Room · DataStore · Kotlin Coroutines\nAll under the Apache 2.0 license. AppWall itself is MIT licensed.",
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = { Text("Privacy", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
            )
        },
    ) { inner ->
        Column(Modifier.fillMaxSize().padding(inner).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp)) {
            PrivacyIllustration(Modifier.size(160.dp).align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(12.dp))
            Text("Nothing leaves your phone. Here's how you can check.", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            Point("No server, no account", "There is nothing to sign in to and nowhere to sync. Your blocklist, schedules and statistics live in a small database inside the app's private storage.")
            Point("The one network permission, explained", "AppWall holds the INTERNET permission for a single reason: strict website blocking must pass lookups that are not blocked on to the DNS resolver your network already uses, and Android refuses to do even that without it. That code lives in one file, DnsFilterVpnService.kt, and nothing else in the app touches the network. It never contacts any other host. With strict blocking off (the default) the app makes no network requests at all.")
            Point("Uninstall means gone", "Backups are disabled. Remove the app and everything it stored is deleted with it.")
            Point("What the accessibility service sees", "Which app is in front, and the website shown in a browser's address bar or in an in-app browser's title bar. It never records keystrokes, messages or page content. The source is public; the whole service is one short file.")
            Point("Strict website blocking (optional)", "Off by default. When on, a local DNS filter also blocks the sites for apps themselves. Android calls it a VPN, but only DNS lookups pass through it. Blocked names are answered locally with \"does not exist\"; everything else is handed to Android's own resolver unchanged. No other traffic touches it, and nothing is logged.")
            Point("What usage access is for", "It powers the Insights tab: how long apps were on screen. Durations only, computed on demand, never stored anywhere else.")
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Point(title: String, body: String) {
    Row(Modifier.padding(vertical = 8.dp)) {
        Column(Modifier.padding(top = 6.dp)) { Spacer(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
