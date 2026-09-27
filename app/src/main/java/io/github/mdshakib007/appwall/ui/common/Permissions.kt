package io.github.mdshakib007.appwall.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.service.AppWallAccessibilityService
import kotlinx.coroutines.delay

data class PermissionStatus(
    val accessibility: Boolean,
    val usage: Boolean,
    val overlay: Boolean,
) {
    /** Blocking apps and websites needs the accessibility service and the overlay. */
    val coreReady get() = accessibility && overlay
    val allGood get() = accessibility && overlay && usage
}

object Permissions {
    fun status(context: Context): PermissionStatus = PermissionStatus(
        accessibility = AppWallAccessibilityService.isEnabled(context),
        usage = Graph.usage.hasPermission(),
        overlay = Settings.canDrawOverlays(context),
    )

    fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun usageIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun overlayIntent(context: Context) =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    fun appInfoIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
