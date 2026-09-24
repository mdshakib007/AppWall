package io.github.mdshakib007.appwall.ui.common

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.service.AppWallAccessibilityService
import io.github.mdshakib007.appwall.service.DnsFilterVpnService
import kotlinx.coroutines.delay

data class PermissionStatus(
    val accessibility: Boolean,
    val usage: Boolean,
    val overlay: Boolean,
    val vpnConsent: Boolean,
    val vpnRunning: Boolean,
    val notifications: Boolean,
    val privateDnsStrict: Boolean,
) {
    /** Apps need accessibility + overlay; websites need the DNS filter consent. */
    val coreReady get() = accessibility && overlay && vpnConsent
    val allGood get() = accessibility && overlay && usage
}

object Permissions {
    fun status(context: Context): PermissionStatus = PermissionStatus(
        accessibility = AppWallAccessibilityService.isEnabled(context),
        usage = Graph.usage.hasPermission(),
        overlay = Settings.canDrawOverlays(context),
        vpnConsent = VpnService.prepare(context) == null,
        vpnRunning = DnsFilterVpnService.running.value,
        notifications = context.getSystemService(NotificationManager::class.java).areNotificationsEnabled(),
        privateDnsStrict = isPrivateDnsStrict(context),
    )

    fun isPrivateDnsStrict(context: Context): Boolean {
        val mode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
        if (mode == "hostname") return true
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val lp = cm.activeNetwork?.let { cm.getLinkProperties(it) } ?: return false
        return Build.VERSION.SDK_INT >= 28 && lp.privateDnsServerName != null
    }

    fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun usageIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun overlayIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun notificationsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun privateDnsIntent() = Intent(Settings.ACTION_WIRELESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun appInfoIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun vpnConsentIntent(context: Context): Intent? = VpnService.prepare(context)
}

/** Re-evaluated every time the screen resumes and every 2s while visible (settings toggles take effect live). */
@Composable
fun rememberPermissionStatus(): State<PermissionStatus> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state = remember { mutableStateOf(Permissions.status(context)) }
    var resumed by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> { resumed = true; state.value = Permissions.status(context) }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(resumed) {
        while (resumed) {
            delay(2000)
            val s = Permissions.status(context)
            if (s != state.value) state.value = s
        }
    }
    return state
}
