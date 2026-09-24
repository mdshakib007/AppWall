@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.mdshakib007.appwall.ui.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Notifications
import io.github.mdshakib007.appwall.service.DnsFilterVpnService

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import io.github.mdshakib007.appwall.ui.Emphasized
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.mdshakib007.appwall.BuildConfig
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.R
import io.github.mdshakib007.appwall.ui.common.BigButton
import io.github.mdshakib007.appwall.ui.common.Pill
import io.github.mdshakib007.appwall.ui.common.PillTone
import io.github.mdshakib007.appwall.ui.common.IconTile
import io.github.mdshakib007.appwall.ui.common.SmallButton
import androidx.compose.ui.text.font.FontWeight
import io.github.mdshakib007.appwall.ui.common.Permissions
import io.github.mdshakib007.appwall.ui.common.SurfaceCard
import io.github.mdshakib007.appwall.ui.common.rememberPermissionStatus
import kotlinx.coroutines.launch

const val GITHUB_URL = "https://github.com/mdshakib007/AppWall"

private class Page(val title: String, val body: String, val art: @Composable (Modifier) -> Unit)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var step by remember { mutableStateOf(0) } // 0 = story pages, 1 = permissions
    AnimatedContent(
        step,
        transitionSpec = {
            (slideInHorizontally(tween(380, easing = Emphasized)) { it } + fadeIn(tween(240))) togetherWith
                (slideOutHorizontally(tween(380, easing = Emphasized)) { -it / 3 } + fadeOut(tween(240)))
        },
        label = "onboarding",
    ) { s ->
        if (s == 0) StoryPages(onContinue = { step = 1 }) else PermissionsStep(onDone = onDone)
    }
}

@Composable
private fun StoryPages(onContinue: () -> Unit) {
    val context = LocalContext.current
    val pages = remember {
        listOf(
            Page("Block what steals your time", "Pick apps and websites. AppWall stops them from opening, in every browser and every in-app browser, on any schedule you like.") { m ->
                Image(painterResource(R.drawable.logo), null, m)
            },
            Page("Nothing leaves your phone", "No account, no server, no analytics, no tracking. There is nowhere for your data to go. Uninstall AppWall and everything is gone.") { m -> PrivacyIllustration(m) },
            Page("Open source, by design", "Every line of code is public. Read it, audit it, improve it. Trust should be verifiable, not promised.") { m -> OpenSourceIllustration(m) },
            Page("Commit with Focus Mode", "When you're serious, lock your whole blocklist for days or weeks. No edits, no exceptions, no way back until it ends.") { m -> FocusIllustration(m) },
        )
    }
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.size - 1

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onContinue) { Text("Skip") }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { i ->
            val p = pages[i]
            // Distance of this page from the viewport centre, in pages: 0 = centred, ±1 = fully off-screen.
            val offset = (pager.currentPage - i) + pager.currentPageOffsetFraction
            Column(
                Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.weight(1f))
                p.art(
                    Modifier.size(240.dp).graphicsLayer {
                        val d = kotlin.math.abs(offset).coerceIn(0f, 1f)
                        translationX = offset * size.width * 0.35f   // parallax: art moves slower than the page
                        scaleX = 1f - 0.12f * d; scaleY = 1f - 0.12f * d
                        alpha = 1f - 0.5f * d
                    },
                )
                Spacer(Modifier.height(40.dp))
                Text(
                    p.title, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = 1f - 0.7f * kotlin.math.abs(offset).coerceIn(0f, 1f) },
                )
                Spacer(Modifier.height(14.dp))
                Text(p.body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (i == 2) {
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }, shape = MaterialTheme.shapes.medium,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, contentColor = MaterialTheme.colorScheme.onSurface),
                    ) { Text("View source on GitHub", fontWeight = FontWeight.SemiBold) }
                }
                Spacer(Modifier.weight(1.3f))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(pages.size) { i ->
                val w by animateDpAsState(if (i == pager.currentPage) 24.dp else 8.dp, label = "dot")
                Box(
                    Modifier.padding(4.dp).height(8.dp).width(w)
                        .background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        BigButton(
            if (last) "Set up protection" else "Next",
            onClick = { if (last) onContinue() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun PermissionsStep(onDone: () -> Unit, standalone: Boolean = false) {
    val context = LocalContext.current
    val perms by rememberPermissionStatus()
    val scope = rememberCoroutineScope()
    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) DnsFilterVpnService.start(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text("Let AppWall do its job", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Blocking needs a few system permissions. Each one is used for exactly one thing, described below. None of them ever sends your data anywhere.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            PermissionCard(
                icon = Icons.Rounded.Accessibility, title = "Accessibility service", required = true, granted = perms.accessibility,
                body = "Lets AppWall see which app is open so it can block the apps you chose. Find AppWall in the list and turn it on.",
                action = { context.startActivity(Permissions.accessibilityIntent()) },
            )
            PermissionCard(
                icon = Icons.Rounded.Layers, title = "Display over other apps", required = true, granted = perms.overlay,
                body = "Shows the “Blocked” screen on top of a blocked app.",
                action = { context.startActivity(Permissions.overlayIntent(context)) },
            )
            PermissionCard(
                icon = Icons.Rounded.Dns, title = "Website blocking", required = true, granted = perms.vpnConsent,
                body = "Blocks websites in every browser and in-app browser by answering their address lookups on the phone. Android calls it a VPN, but no traffic leaves through it and no server is involved.",
                action = { Permissions.vpnConsentIntent(context)?.let { vpnLauncher.launch(it) } ?: DnsFilterVpnService.start(context) },
            )
            PermissionCard(
                icon = Icons.Rounded.BarChart, title = "Usage access", required = false, granted = perms.usage,
                body = "Powers the Insights tab: screen time per app and how much time you're getting back.",
                action = { context.startActivity(Permissions.usageIntent()) },
            )
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                PermissionCard(
                    icon = Icons.Rounded.Notifications, title = "Notifications", required = false, granted = perms.notifications,
                    body = "Only for the silent “website blocking is on” status notification Android requires.",
                    action = { notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(Modifier.padding(horizontal = 24.dp)) {
            BigButton(
                if (perms.coreReady) "Continue" else "Continue anyway",
                onClick = {
                    scope.launch {
                        Graph.prefs.setOnboardingDone(true)
                        if (perms.vpnConsent) DnsFilterVpnService.start(context)
                        onDone()
                    }
                },
            )
            if (!standalone) {
                Text(
                    if (perms.coreReady) "You're all set." else "You can finish this later from the home screen.",
                    Modifier.fillMaxWidth().padding(vertical = 10.dp), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, required: Boolean, granted: Boolean, body: String, action: () -> Unit) {
    SurfaceCard(Modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = { if (!granted) action() }) {
        Row(Modifier.padding(14.dp).animateContentSize(), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, size = 40.dp, muted = granted)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (required && !granted) {
                    Spacer(Modifier.height(4.dp))
                    Pill("Required", tone = PillTone.PRIMARY)
                }
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(10.dp))
            AnimatedContent(
                granted,
                transitionSpec = { (scaleIn(tween(320, easing = Emphasized), initialScale = 0.4f) + fadeIn(tween(200))) togetherWith fadeOut(tween(120)) },
                label = "grant",
            ) { ok ->
                if (ok) Icon(Icons.Rounded.CheckCircle, "Granted", tint = io.github.mdshakib007.appwall.ui.theme.AppWallColors.success)
                else SmallButton("Allow", action)
            }
        }
    }
}
