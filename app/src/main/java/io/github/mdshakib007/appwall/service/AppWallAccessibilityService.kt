package io.github.mdshakib007.appwall.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.InputMethodManager
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.Domains
import io.github.mdshakib007.appwall.data.Catalog
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.ui.blocked.BlockedActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The eyes of AppWall. It only ever looks at two things:
 *  1. which package owns the window in front (to block apps),
 *  2. the text of the address bar in browsers (to block websites).
 * It never reads anything else, never stores page content, and cannot send anything anywhere.
 */
class AppWallAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AppWallA11y"
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
            val me = "${context.packageName}/${AppWallAccessibilityService::class.java.name}"
            return flat.split(':').any { it.equals(me, ignoreCase = true) }
        }

        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings", "com.samsung.android.settings", "com.miui.securitycenter",
            "com.google.android.permissioncontroller", "com.android.permissioncontroller",
            "com.android.packageinstaller", "com.google.android.packageinstaller",
        )
        private val SETTINGS_DANGER_WORDS = listOf(
            "uninstall", "force stop", "turn off", "disconnect", "forget", "disable", "off", "clear storage", "clear data", "shortcut",
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var imePackages: Set<String> = emptySet()
    private var lastShownKey: String? = null
    private var lastShownAt = 0L
    private var lastBrowserCheckAt = 0L
    private var lastSettingsCheckAt = 0L

    // Website time tracking (for Insights): which domain is currently visible in a browser.
    private var currentSite: String? = null
    private var currentSiteSince = 0L
    private var currentForeground: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Graph.init(this)
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 80
        }
        refreshImes()
        _connected.value = true
        Log.i(TAG, "connected")
    }

    private fun refreshImes() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imePackages = runCatching { imm.enabledInputMethodList.map { it.packageName }.toSet() }.getOrDefault(emptySet())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui" || pkg in imePackages) return
        val state = Graph.engine.state
        val now = System.currentTimeMillis()

        // 1. Foreground app changed?
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != currentForeground) {
                currentForeground = pkg
                Graph.foregroundPackage = pkg
                if (!isBrowser(pkg)) endSiteSession(now)
            }
            state.blockedItemForPackage(pkg)?.let { item ->
                block(item, pkg, isApp = true)
                return
            }
        }

        // 2. Browser address bar?
        if (isBrowser(pkg)) {
            val mono = SystemClock.uptimeMillis()
            if (mono - lastBrowserCheckAt < 250) return
            lastBrowserCheckAt = mono
            val host = readAddressBarHost(pkg)
            if (host != null) {
                state.blockedItemForHost(host)?.let { item ->
                    endSiteSession(now)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    block(item, host, isApp = false)
                    return
                }
                trackSite(host, now)
            }
            return
        }

        // 3. Focus-mode guard: keep the user out of AppWall's own Settings pages.
        if (state.focusActive && pkg in SETTINGS_PACKAGES) {
            val mono = SystemClock.uptimeMillis()
            if (mono - lastSettingsCheckAt < 400) return
            lastSettingsCheckAt = mono
            if (settingsPageTargetsAppWall()) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                mainHandler.postDelayed({ showProtected() }, 250)
            }
        }
    }

    private fun isBrowser(pkg: String): Boolean =
        Catalog.browserUrlBarIds.containsKey(pkg) || pkg in Graph.installedApps.browserPackages()

    private fun block(item: BlockItem, key: String, isApp: Boolean) {
        val mono = SystemClock.uptimeMillis()
        if (lastShownKey == key && mono - lastShownAt < 1500) return
        lastShownKey = key
        lastShownAt = mono
        Graph.scope.launch { Graph.repo.recordAttempt(item) }
        // Kick the offender out first (HOME for apps; the browser already got BACK), then show the Blocked
        // screen a beat later so it lands on top of the launcher instead of racing the HOME action.
        if (isApp) performGlobalAction(GLOBAL_ACTION_HOME)
        if (Settings.canDrawOverlays(this)) {
            mainHandler.postDelayed({ BlockedActivity.show(this, item, if (isApp) null else key) }, if (isApp) 250 else 0)
        }
    }

    private fun showProtected() {
        val mono = SystemClock.uptimeMillis()
        if (lastShownKey == "__settings" && mono - lastShownAt < 1500) return
        lastShownKey = "__settings"; lastShownAt = mono
        BlockedActivity.showProtected(this)
    }

    // --- Address bar reading -----------------------------------------------------------------------

    private fun readAddressBarHost(pkg: String): String? {
        val root = rootInActiveWindow ?: return null
        try {
            Catalog.browserUrlBarIds[pkg]?.forEach { id ->
                val nodes = root.findAccessibilityNodeInfosByViewId(id)
                for (n in nodes) {
                    val text = n.text?.toString()
                    n.recycleCompat()
                    val host = text?.let { Domains.hostFromAddressBar(it) }
                    if (host != null) return host
                }
            }
            // Unknown browser: look for an editable field whose text looks like a URL.
            return findUrlHeuristically(root, 0)
        } finally {
            root.recycleCompat()
        }
    }

    private fun findUrlHeuristically(node: AccessibilityNodeInfo, depth: Int): String? {
        if (depth > 12) return null
        val cls = node.className?.toString() ?: ""
        if (cls.endsWith("EditText") || cls.endsWith("TextView") && node.isClickable) {
            node.text?.toString()?.let { t -> Domains.hostFromAddressBar(t)?.let { return it } }
        }
        val count = node.childCount
        for (i in 0 until minOf(count, 40)) {
            val child = node.getChild(i) ?: continue
            val r = findUrlHeuristically(child, depth + 1)
            child.recycleCompat()
            if (r != null) return r
        }
        return null
    }

    private fun settingsPageTargetsAppWall(): Boolean {
        val root = rootInActiveWindow ?: return false
        try {
            val texts = ArrayList<String>(64)
            collectTexts(root, texts, 0)
            val mentionsUs = texts.any { it.contains("AppWall", ignoreCase = true) }
            if (!mentionsUs) return false
            val lower = texts.joinToString("\n").lowercase()
            return SETTINGS_DANGER_WORDS.any { w -> lower.contains(w) }
        } finally {
            root.recycleCompat()
        }
    }

    private fun collectTexts(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int) {
        if (depth > 14 || out.size > 200) return
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
        for (i in 0 until minOf(node.childCount, 60)) {
            val c = node.getChild(i) ?: continue
            collectTexts(c, out, depth + 1)
            c.recycleCompat()
        }
    }

    // --- Site time tracking --------------------------------------------------------------------------

    private fun trackSite(host: String, now: Long) {
        val domain = registrable(host)
        if (domain == currentSite) return
        endSiteSession(now)
        currentSite = domain
        currentSiteSince = now
    }

    private fun endSiteSession(now: Long) {
        val d = currentSite ?: return
        val since = currentSiteSince
        currentSite = null
        if (now - since >= 1000) Graph.scope.launch { Graph.repo.recordSiteSession(d, since, now) }
    }

    /** Crude eTLD+1: keeps last two labels, or three for known second-level suffixes like co.uk / com.bd. */
    private fun registrable(host: String): String {
        val parts = host.split('.')
        if (parts.size <= 2) return host
        val sld = parts[parts.size - 2]
        val twoLevel = sld in setOf("co", "com", "net", "org", "gov", "edu", "ac") && parts.last().length == 2
        return if (twoLevel) parts.takeLast(3).joinToString(".") else parts.takeLast(2).joinToString(".")
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        _connected.value = false
        endSiteSession(System.currentTimeMillis())
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _connected.value = false
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleCompat() {
        if (android.os.Build.VERSION.SDK_INT < 33) runCatching { recycle() }
    }
}
