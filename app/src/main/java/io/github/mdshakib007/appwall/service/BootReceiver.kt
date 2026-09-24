package io.github.mdshakib007.appwall.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import io.github.mdshakib007.appwall.Graph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** After reboot / app update: bring the DNS filter back if the user had it on and consent still stands. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Graph.init(context)
        val pending = goAsync()
        Graph.scope.launch {
            try {
                val wanted = Graph.prefs.dnsFilterEnabled.first()
                val consented = VpnService.prepare(context) == null
                if (wanted && consented) DnsFilterVpnService.start(context)
            } finally {
                pending.finish()
            }
        }
    }
}
