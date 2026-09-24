package io.github.mdshakib007.appwall.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.Network
import android.net.NetworkCapabilities
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.mdshakib007.appwall.BuildConfig
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.MainActivity
import io.github.mdshakib007.appwall.R
import io.github.mdshakib007.appwall.core.Dns
import io.github.mdshakib007.appwall.core.Domains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A DNS-only "VPN". Only one IP (our fake resolver 10.111.222.3) is routed into the tunnel, so the
 * *only* packets that ever reach this service are DNS queries. Everything else flows normally.
 *
 * Blocked domain (or subdomain) -> NXDOMAIN. Anything else -> forwarded unchanged to the DNS server the
 * phone was already using, and the answer is handed back. Nothing is logged, nothing is stored, nothing is
 * sent anywhere else. This file is the ONLY place in AppWall that touches the network.
 */
class DnsFilterVpnService : VpnService() {

    companion object {
        private const val TAG = "AppWallDns"
        const val ACTION_START = "io.github.mdshakib007.appwall.START_DNS"
        const val ACTION_STOP = "io.github.mdshakib007.appwall.STOP_DNS"
        private const val CHANNEL = "dns_filter"
        private const val NOTIF_ID = 1001
        private const val TUN_ADDRESS = "10.111.222.1"
        private const val TUN_ADDRESS6 = "fd00:6170:7077:616c::1"
        const val FAKE_DNS = "10.111.222.3"
        private const val MAX_PACKET = 32767

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        /** Set when Android revoked us (another VPN was started). UI shows a warning. */
        private val _revoked = MutableStateFlow(false)
        val revoked: StateFlow<Boolean> = _revoked

        fun start(context: Context) {
            val i = Intent(context, DnsFilterVpnService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(i) else context.startService(i)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, DnsFilterVpnService::class.java).setAction(ACTION_STOP))
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tun: ParcelFileDescriptor? = null
    private var readerThread: Thread? = null
    private val forwardPool = Executors.newCachedThreadPool { r -> Thread(r, "appwall-dns-fwd").apply { isDaemon = true } }
    private val stopping = AtomicBoolean(false)
    private var restartJob: Job? = null
    @Volatile private var upstream: List<InetAddress> = emptyList()
    @Volatile private var underlyingNets: List<Network> = emptyList()
    private lateinit var connectivity: ConnectivityManager
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
        connectivity = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { shutdown(); return START_NOT_STICKY }
            else -> {
                startAsForeground()
                if (tun == null) {
                    watchNetworks()
                    establish()
                    observeBlocklist()
                }
                return START_STICKY
            }
        }
    }

    private fun startAsForeground() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val ch = NotificationChannel(CHANNEL, getString(R.string.notification_channel_vpn), NotificationManager.IMPORTANCE_MIN).apply {
            setShowBadge(false)
            description = "Shown while the on-device website filter is running"
        }
        nm.createNotificationChannel(ch)
    }

    private fun buildNotification(): Notification {
        val count = Graph.engine.state.itemByDomain.size
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(getString(R.string.notification_vpn_title))
            .setContentText(getString(R.string.notification_vpn_text, count))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .build()
    }

    private fun updateNotification() {
        runCatching { getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification()) }
    }

    // --- Underlying network / upstream DNS ---------------------------------------------------------

    private fun watchNetworks() {
        refreshUpstream()
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshUpstream()
            override fun onLost(network: Network) = refreshUpstream()
            override fun onLinkPropertiesChanged(network: Network, lp: android.net.LinkProperties) = refreshUpstream()
        }
        netCallback = cb
        runCatching { connectivity.registerNetworkCallback(android.net.NetworkRequest.Builder().build(), cb) }
    }

    /** Collects DNS servers of every non-VPN network that has internet. */
    private fun refreshUpstream() {
        val servers = LinkedHashSet<InetAddress>()
        val underlying = ArrayList<Network>()
        for (n in connectivity.allNetworks) {
            val caps = connectivity.getNetworkCapabilities(n) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) continue
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) continue
            val lp = connectivity.getLinkProperties(n) ?: continue
            if (lp.dnsServers.isNotEmpty()) underlying += n
            servers += lp.dnsServers
        }
        upstream = servers.toList()
        underlyingNets = underlying
        if (underlying.isNotEmpty()) runCatching { setUnderlyingNetworks(underlying.toTypedArray()) }
        Log.d(TAG, "upstream DNS: $upstream")
    }

    // --- Tunnel ----------------------------------------------------------------------------------

    private fun establish() {
        stopping.set(false)
        val builder = Builder()
            .setSession(getString(R.string.app_name))
            .addAddress(TUN_ADDRESS, 32)
            // An IPv6 address with no IPv6 routes tells Android "IPv6 is fine, leave it alone". Without it,
            // Android adds an "::/0 unreachable" route and breaks IPv6 for every app while we run.
            .addAddress(TUN_ADDRESS6, 128)
            .addRoute(FAKE_DNS, 32)          // ONLY the fake resolver goes through the tunnel
            .addDnsServer(FAKE_DNS)
            .setMtu(1500)
            .setBlocking(true)
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(false)
        runCatching { builder.addDisallowedApplication(packageName) }
        val pfd = try {
            builder.establish()
        } catch (t: Throwable) {
            Log.e(TAG, "establish failed", t); null
        }
        if (pfd == null) { shutdown(); return }
        tun = pfd
        _running.value = true
        _revoked.value = false
        readerThread = Thread({ readLoop(pfd) }, "appwall-dns-tun").apply { isDaemon = true; start() }
    }

    private fun readLoop(pfd: ParcelFileDescriptor) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buf = ByteArray(MAX_PACKET)
        while (!stopping.get() && tun === pfd) {
            val n = try { input.read(buf) } catch (e: Exception) { if (!stopping.get()) Log.w(TAG, "tun read: $e"); break }
            if (n <= 0) continue
            val udp = Dns.parseIpv4Udp(buf, n) ?: continue
            if (udp.dstPort != 53) continue
            val q = Dns.parseQuestion(udp.payload)
            if (q == null) { forward(udp, output); continue }
            val blocked = Domains.findBlocked(q.name, Graph.engine.state.blockedDomains)
            if (blocked != null) {
                val resp = Dns.buildNxDomain(udp.payload)
                write(output, Dns.buildIpv4Udp(udp.dstIp, udp.srcIp, udp.dstPort, udp.srcPort, resp))
                Graph.scope.launch { Graph.engine.state.itemByDomain[blocked]?.let { Graph.repo.recordAttempt(it) } }
            } else {
                forward(udp, output)
            }
        }
    }

    /**
     * Hands the untouched query to Android's own resolver on the underlying (real) network and relays the answer.
     * Using the system resolver means we change nothing about how lookups happen (Private DNS, caching, IPv6
     * all behave exactly as without AppWall). Android 10+.
     */
    private fun forward(udp: Dns.UdpPacket, output: FileOutputStream) {
        val net = underlyingNets.firstOrNull()
        if (net == null || Build.VERSION.SDK_INT < 29) return
        try {
            DnsResolver.getInstance().rawQuery(
                net, udp.payload, DnsResolver.FLAG_EMPTY, forwardPool, null,
                object : DnsResolver.Callback<ByteArray> {
                    override fun onAnswer(answer: ByteArray, rcode: Int) {
                        if (answer.size < 12) return
                        // Keep the transaction id the client used.
                        answer[0] = udp.payload[0]; answer[1] = udp.payload[1]
                        write(output, Dns.buildIpv4Udp(udp.dstIp, udp.srcIp, udp.dstPort, udp.srcPort, answer))
                    }

                    override fun onError(error: DnsResolver.DnsException) {
                        if (BuildConfig.DEBUG) Log.w(TAG, "resolver error code=${error.code} cause=${error.cause}")
                        write(output, Dns.buildIpv4Udp(udp.dstIp, udp.srcIp, udp.dstPort, udp.srcPort, Dns.buildServFail(udp.payload)))
                    }
                },
            )
        } catch (e: Exception) {
            Log.w(TAG, "forward: $e")
        }
    }

    private fun write(output: FileOutputStream, packet: ByteArray) {
        synchronized(output) { runCatching { output.write(packet) } }
    }

    /** Re-establishing the tunnel is a "network change" that flushes OS + browser DNS caches. */
    private fun observeBlocklist() {
        scope.launch {
            Graph.engine.stateFlow
                .map { it.itemByDomain.keys }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { domains ->
                    updateNotification()
                    if (tun != null) {
                        delay(1500)
                        reestablish()
                    }
                }
        }
    }

    private fun reestablish() {
        val old = tun ?: return
        tun = null
        runCatching { old.close() }
        establish()
    }

    override fun onRevoke() {
        _revoked.value = true
        shutdown()
    }

    private fun shutdown() {
        stopping.set(true)
        val old = tun
        tun = null
        runCatching { old?.close() }
        _running.value = false
        netCallback?.let { runCatching { connectivity.unregisterNetworkCallback(it) } }
        netCallback = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopping.set(true)
        runCatching { tun?.close() }
        tun = null
        _running.value = false
        scope.cancel()
        forwardPool.shutdownNow()
        super.onDestroy()
    }
}
