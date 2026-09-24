package io.github.mdshakib007.appwall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.net.VpnService
import io.github.mdshakib007.appwall.service.DnsFilterVpnService
import io.github.mdshakib007.appwall.ui.AppRoot
import kotlinx.coroutines.launch
import io.github.mdshakib007.appwall.ui.theme.AppWallTheme
import io.github.mdshakib007.appwall.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        Graph.init(this)
        enableEdgeToEdge()
        // Read once before drawing so we never flash the wrong start screen. DataStore is tiny; this is ~1ms.
        val onboardingDone = runBlocking { Graph.prefs.onboardingDone.first() }
        splash.setKeepOnScreenCondition { false }
        setContent {
            val mode by Graph.prefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            AppWallTheme(mode) { AppRoot(onboardingDone) }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the process was killed (task manager, force stop, low memory) the DNS filter is gone until the next
        // reboot. Bring it back whenever the user opens the app, as long as they want it and consent still stands.
        Graph.scope.launch {
            val wanted = Graph.prefs.dnsFilterEnabled.first()
            if (wanted && !DnsFilterVpnService.running.value && VpnService.prepare(this@MainActivity) == null) {
                DnsFilterVpnService.start(this@MainActivity)
            }
        }
    }
}
