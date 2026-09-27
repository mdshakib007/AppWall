package io.github.mdshakib007.appwall.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.InputMethodManager
import io.github.mdshakib007.appwall.Graph
import io.github.mdshakib007.appwall.core.Domains
import io.github.mdshakib007.appwall.data.Catalog
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.ui.blocked.BlockedActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The whole enforcement layer of AppWall. It looks at exactly three things:
 *
 *  1. which package owns the window in front, to block apps (and to guard Settings during Focus Mode);
 *  2. the address bar of browsers, to block websites: the moment a blocked address is *committed* (typed and
 *     entered, or reached through a link) the navigation is cancelled with BACK, so the page never shows. Nothing
 *     is drawn over the browser and nothing happens while the user is still typing in the address bar;
 *  3. the title strip of in-app browsers (Messenger, Facebook, Instagram, ...), for the same purpose.
 *
 * It never reads page content, never records what is typed, and has no way to send anything anywhere: the app
 * holds no network permission at all.
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
            "uninstall", "force stop", "turn off", "disable", "off", "clear storage", "clear data", "shortcut",
        )

        /** How often at most we read a browser's address bar / an app's title strip. */
        private const val BROWSER_CHECK_GAP_MS = 200L
        private const val IN_APP_CHECK_GAP_MS = 300L
        /** After each BACK, wait this long before checking whether the blocked page is really gone. */
        private const val VERIFY_DELAY_MS = 250L
        /** URL loaded into a browser we had to bring back after BACK minimised it (see [recoverMinimisedBrowser]). */
        private const val BLANK_URL = "about:blank"
        /** Give up on BACK after this many tries and send the browser home instead. */
        private const val MAX_BACKS = 4
        /** Only this part of the screen (from the top) is searched for an in-app browser's address strip. */
        private const val STRIP_FRACTION = 0.30f
        /** An in-app browser's web view must cover at least this much of the screen height. */
        private const val MIN_WEBVIEW_FRACTION = 0.35f
        /** Bundle keys under which Chromium exposes a web node's document URL / link target. */
        private val WEB_URL_EXTRA_KEYS = listOf("AccessibilityNodeInfo.url", "AccessibilityNodeInfo.targetUrl")
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val rect = Rect()
    private var imePackages: Set<String> = emptySet()
    private var screenHeight = 0
    private var stateJob: Job? = null

    private var currentForeground: String? = null
    private var homePackage: String? = null
    private var lastShownKey: String? = null
    private var lastShownAt = 0L
    private var lastSettingsCheckAt = 0L

    // Coalesced checks: many events arrive per navigation; we read the window at most every N ms, but always
    // once more after the last event of a burst so nothing is missed.
    private var browserCheckPending = false
    private var lastBrowserCheckAt = 0L
    private var pendingBrowserPkg: String? = null
    private var inAppCheckPending = false
    private var lastInAppCheckAt = 0L
    private var pendingInAppPkg: String? = null

    /** A website block in progress: BACK has been sent, we are waiting to confirm the page is gone. */
    private class Enforcement(val pkg: String, val host: String, val inApp: Boolean, val customTab: Boolean) {
        var backs = 0
        val startedAt = SystemClock.uptimeMillis()
    }
    private var enforcing: Enforcement? = null
    /** Last time BACK made a browser leave the screen: the same browser and site again within 15 s means it is bouncing. */
    private var lastGoneKey: String? = null
    private var lastGoneAt = 0L
    /** Package -> whether its front window is a Custom Tab (Chrome's CustomTabActivity, Firefox's ExternalAppBrowserActivity). */
    private val customTabFront = HashMap<String, Boolean>(4)
    private enum class Outcome { BLOCKED, CLEAR, GONE }

    // Website time tracking (Insights): which domain is currently visible in a browser.
    private var currentSite: String? = null
    private var currentSiteSince = 0L

    /** What a browser's address bar currently shows. [editing] = the user is typing in it right now. */
    private class AddressBar(val host: String?, val editing: Boolean)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Graph.init(this)
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 50
        }
        refreshImes()
        screenHeight = resources.displayMetrics.heightPixels
        homePackage = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), android.content.pm.PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
        Log.i(TAG, "home launcher: $homePackage")
        _connected.value = true
        // Re-evaluate whatever is in front whenever the rules change (item added, schedule boundary, focus started).
        stateJob?.cancel()
        stateJob = Graph.scope.launch { Graph.engine.stateFlow.collect { mainHandler.post { recheck() } } }
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
                if (!isBrowser(pkg)) endSiteSession(now)
            }
            event.className?.toString()?.takeIf { it.endsWith("Activity") }?.let { cls ->
                customTabFront[pkg] = cls.contains("CustomTab") || cls.contains("ExternalAppBrowser")
            }
            state.blockedItemForPackage(pkg)?.let { item ->
                blockApp(item, pkg)
                return
            }
        }

        // 2. Websites: real browsers by address bar, everything else by in-app browser title strip.
        if (isBrowser(pkg)) {
            scheduleBrowserCheck(pkg)
            return
        }
        if (state.blockedDomains.isNotEmpty() && pkg !in SETTINGS_PACKAGES && pkg != homePackage) scheduleInAppCheck(pkg)

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

    /** Rules changed: look again at whatever is in front right now. */
    private fun recheck() {
        // Ask the system what is in front rather than trusting the last event: right after (re)connecting, or a
        // reboot, no window event has been seen yet, but a blocked page may already be on screen.
        val active = rootInActiveWindow
        val activePkg = active?.packageName?.toString()
        active?.recycleCompat()
        val pkg = activePkg?.takeUnless { it == packageName || it == "com.android.systemui" || it in imePackages }
            ?: currentForeground ?: return
        if (pkg != currentForeground) { currentForeground = pkg; if (!isBrowser(pkg)) endSiteSession(System.currentTimeMillis()) }
        val state = Graph.engine.state
        state.blockedItemForPackage(pkg)?.let { blockApp(it, pkg); return }
        if (isBrowser(pkg)) scheduleBrowserCheck(pkg)
        else if (state.blockedDomains.isNotEmpty() && pkg !in SETTINGS_PACKAGES && pkg != homePackage) scheduleInAppCheck(pkg)
    }

    private fun isBrowser(pkg: String): Boolean =
        Catalog.browserUrlBarIds.containsKey(pkg) || pkg in Graph.installedApps.browserPackages()

    // --- Apps ----------------------------------------------------------------------------------------

    private fun blockApp(item: BlockItem, pkg: String) {
        val mono = SystemClock.uptimeMillis()
        if (lastShownKey == pkg && mono - lastShownAt < 1500) return
        lastShownKey = pkg
        lastShownAt = mono
        Graph.scope.launch { Graph.repo.recordAttempt(item) }
        // Kick the app out first, then show the Blocked screen a beat later so it lands on top of the launcher
        // instead of racing the HOME action.
        performGlobalAction(GLOBAL_ACTION_HOME)
        if (Settings.canDrawOverlays(this)) {
            mainHandler.postDelayed({ BlockedActivity.show(this, item) }, 250)
        }
    }

    private fun showProtected() {
        val mono = SystemClock.uptimeMillis()
        if (lastShownKey == "__settings" && mono - lastShownAt < 1500) return
        lastShownKey = "__settings"; lastShownAt = mono
        BlockedActivity.showProtected(this)
    }

    // --- Websites: scheduling ------------------------------------------------------------------------

    private fun scheduleBrowserCheck(pkg: String) {
        pendingBrowserPkg = pkg
        if (browserCheckPending) return
        browserCheckPending = true
        val wait = (lastBrowserCheckAt + BROWSER_CHECK_GAP_MS - SystemClock.uptimeMillis()).coerceIn(0L, BROWSER_CHECK_GAP_MS)
        mainHandler.postDelayed({
            browserCheckPending = false
            lastBrowserCheckAt = SystemClock.uptimeMillis()
            pendingBrowserPkg?.let { checkBrowser(it) }
        }, wait)
    }

    private fun scheduleInAppCheck(pkg: String) {
        pendingInAppPkg = pkg
        if (inAppCheckPending) return
        inAppCheckPending = true
        val wait = (lastInAppCheckAt + IN_APP_CHECK_GAP_MS - SystemClock.uptimeMillis()).coerceIn(0L, IN_APP_CHECK_GAP_MS)
        mainHandler.postDelayed({
            inAppCheckPending = false
            lastInAppCheckAt = SystemClock.uptimeMillis()
            pendingInAppPkg?.let { checkInApp(it) }
        }, wait)
    }

    // --- Websites: detection -------------------------------------------------------------------------

    private fun checkBrowser(pkg: String) {
        if (enforcing != null) return // already kicking a page out; the verify step re-reads the bar
        val bar = readAddressBar(pkg) ?: return
        if (bar.editing) return // the user is typing; a URL is only a visit once it is committed
        val host = bar.host ?: return
        val item = Graph.engine.state.blockedItemForHost(host)
        if (item != null) startEnforcement(pkg, item, host, inApp = false)
        else trackSite(host, System.currentTimeMillis())
    }

    private fun checkInApp(pkg: String) {
        if (enforcing != null) return
        val state = Graph.engine.state
        if (state.blockedDomains.isEmpty()) return
        val root = windowRootFor(pkg) ?: return
        try {
            val host = detectInAppHost(root) ?: return
            val item = state.blockedItemForHost(host) ?: return
            startEnforcement(pkg, item, host, inApp = true)
        } finally {
            root.recycleCompat()
        }
    }

    /**
     * Which site an in-app browser in this window is showing, or null if there is none.
     *  a) a bare host in the title strip with a big web view under it (most in-app browsers), or
     *  b) for browsers that hide their address from accessibility (Facebook's / Messenger's), the URL the web
     *     content itself reports: Chromium attaches the document URL and link targets to web nodes as extras.
     * A link pasted in a chat, or a post caption, never comes with a big web view, so neither path fires there.
     */
    private fun detectInAppHost(root: AccessibilityNodeInfo): String? {
        findHostInTopStrip(root)?.let { hit ->
            if (hasLargeWebViewBelow(root, hit.top)) return hit.host
        }
        val webView = findLargeWebView(root) ?: return null
        try {
            return hostFromWebContent(webView)
        } finally {
            webView.recycleCompat()
        }
    }

    private fun findLargeWebView(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val minHeight = (screenHeight * MIN_WEBVIEW_FRACTION).toInt()
        var found: AccessibilityNodeInfo? = null
        val budget = intArrayOf(600)
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (found != null || depth > 22 || --budget[0] < 0) return
            if (isWebView(node)) {
                node.getBoundsInScreen(rect)
                if (rect.height() >= minHeight) found = AccessibilityNodeInfo.obtain(node)
                return
            }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (found != null) return
            }
        }
        walk(root, 0)
        return found
    }

    /**
     * Reads the site out of Chromium web content: the document URL if the root web node carries one, otherwise
     * the host that most absolute link targets on the page point to (at least 3 links and a clear majority).
     */
    private fun hostFromWebContent(webView: AccessibilityNodeInfo): String? {
        val counts = HashMap<String, Int>()
        var total = 0
        var documentHost: String? = null
        val budget = intArrayOf(160)
        val diag = StringBuilder()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (documentHost != null || depth > 14 || --budget[0] < 0) return
            val extras = node.extras
            if (depth <= 2 && diag.length < 600) diag.append(depth).append(':').append(extras.keySet().joinToString(",")).append(' ')
            for (key in WEB_URL_EXTRA_KEYS) {
                val value = extras.getCharSequence(key)?.toString() ?: continue
                val host = Domains.hostFromAddressBar(value) ?: continue
                if (depth <= 1 && key == "AccessibilityNodeInfo.url") { documentHost = host; return }
                counts[host] = (counts[host] ?: 0) + 1
                total++
            }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (documentHost != null) return
            }
        }
        walk(webView, 0)
        Log.i(TAG, "web content: document=$documentHost links=$counts extras=[$diag]")
        documentHost?.let { return it }
        val best = counts.maxByOrNull { it.value } ?: return null
        return if (best.value >= 3 && best.value * 100 >= total * 60) best.key else null
    }

    // --- Websites: enforcement -----------------------------------------------------------------------

    /**
     * Gets the blocked page out of sight without drawing anything over the browser: BACK cancels a navigation
     * that is still pending and returns an already-open page to the previous one. We then look again and repeat a
     * few times (a dialog or the keyboard may have eaten the first BACK), and as a last resort send the app home.
     */
    private fun startEnforcement(pkg: String, item: BlockItem, host: String, inApp: Boolean) {
        endSiteSession(System.currentTimeMillis())
        enforcing = Enforcement(pkg, host, inApp, customTab = customTabFront[pkg] == true)
        Graph.scope.launch { Graph.repo.recordAttempt(item) }
        Log.i(TAG, "blocking $host in $pkg")
        sendBackAndVerify()
    }

    private fun sendBackAndVerify() {
        val e = enforcing ?: return
        e.backs++
        performGlobalAction(GLOBAL_ACTION_BACK)
        mainHandler.postDelayed(::verifyEnforcement, VERIFY_DELAY_MS)
    }

    private fun verifyEnforcement() {
        val e = enforcing ?: return
        val t0 = SystemClock.uptimeMillis()
        val outcome = outcome(e)
        Log.i(TAG, "verify ${e.host}: $outcome after ${e.backs} back(s), ${t0 - e.startedAt} ms since start, read took ${SystemClock.uptimeMillis() - t0} ms")
        when (outcome) {
            Outcome.CLEAR -> enforcing = null
            Outcome.GONE -> {
                enforcing = null
                // BACK on a tab with no history makes Chrome (and others) minimise, keeping the blocked tab as the
                // current one: the next launch would show it again and bounce again, forever. If that is what just
                // happened (we are looking at the launcher, or the same browser bounced off the same site a moment
                // ago), bring the browser back on a blank tab instead. A Custom Tab, or a tab another app opened for
                // a link, is simply closed by BACK and the user is back in that app: nothing more to do.
                val mono = SystemClock.uptimeMillis()
                val key = e.pkg + "|" + e.host
                val bouncing = lastGoneKey == key && mono - lastGoneAt < 15_000
                lastGoneKey = key; lastGoneAt = mono
                if (!e.inApp && !e.customTab && (bouncing || foregroundIsHome())) recoverMinimisedBrowser(e.pkg)
                else Log.i(TAG, "${e.pkg} left the screen")
            }
            Outcome.BLOCKED -> if (e.backs < MAX_BACKS) sendBackAndVerify() else {
                enforcing = null
                Log.w(TAG, "BACK did not clear ${e.host}; sending ${e.pkg} home")
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
    }

    private fun outcome(e: Enforcement): Outcome {
        val state = Graph.engine.state
        // Decide "gone" from the active window alone. Scanning all windows here can stall for seconds on the
        // browser's disappearing window and then report a stale root, which misses the minimised case.
        val active = rootInActiveWindow ?: return Outcome.GONE
        val activePkg = active.packageName?.toString()
        if (activePkg != e.pkg) { active.recycleCompat(); return Outcome.GONE }
        val root = active
        try {
            if (e.inApp) {
                val host = detectInAppHost(root) ?: return Outcome.CLEAR
                return if (state.blockedItemForHost(host) != null) Outcome.BLOCKED else Outcome.CLEAR
            }
            val bar = readAddressBar(e.pkg, root) ?: return Outcome.CLEAR
            if (bar.editing || bar.host == null) return Outcome.CLEAR
            return if (state.blockedItemForHost(bar.host) != null) Outcome.BLOCKED else Outcome.CLEAR
        } finally {
            root.recycleCompat()
        }
    }

    private fun foregroundIsHome(): Boolean {
        val home = homePackage ?: return false
        val active = rootInActiveWindow
        if (active != null) {
            val p = active.packageName?.toString()
            active.recycleCompat()
            if (p != null) return p == home
        }
        return currentForeground == home
    }

    /**
     * Re-opens [pkg] on a blank page. The intent carries our package as the "application id", which browsers use
     * to reuse the tab they created for us last time instead of piling up blank tabs. The blocked tab stays in the
     * tab list (no app can close another app's tabs) but is no longer the one on screen.
     */
    private fun recoverMinimisedBrowser(pkg: String) {
        Log.i(TAG, "$pkg minimised with the blocked tab still current; reopening it on a blank tab")
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(BLANK_URL))
            .setPackage(pkg)
            .putExtra("com.android.browser.application_id", packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(intent) }.onFailure { Log.w(TAG, "could not reopen $pkg: $it") }
    }

    // --- Reading windows -----------------------------------------------------------------------------

    /** Root of [pkg]'s window: the active one if it belongs to [pkg], otherwise any visible app window of [pkg]. */
    private fun windowRootFor(pkg: String): AccessibilityNodeInfo? {
        rootInActiveWindow?.let { active ->
            if (active.packageName?.toString() == pkg) return active
            active.recycleCompat()
        }
        val list = runCatching { windows }.getOrNull() ?: return null
        for (w in list) {
            if (w.type != AccessibilityWindowInfo.TYPE_APPLICATION) continue
            val r = w.root ?: continue
            if (r.packageName?.toString() == pkg) return r
            r.recycleCompat()
        }
        return null
    }

    private fun readAddressBar(pkg: String): AddressBar? {
        val root = windowRootFor(pkg) ?: return null
        try {
            return readAddressBar(pkg, root)
        } finally {
            root.recycleCompat()
        }
    }

    private fun readAddressBar(pkg: String, root: AccessibilityNodeInfo): AddressBar? {
        val ids = Catalog.browserUrlBarIds[pkg]
            ?: return findUrlFieldInEdges(root) // no view id known: URL-looking field along the top or bottom edge
        for (id in ids) {
            if (!id.contains(":id/")) continue // Compose tags can't be looked up this way, see below
            for (n in root.findAccessibilityNodeInfosByViewId(id)) {
                try {
                    readUrlNode(n)?.let { return it }
                } finally {
                    n.recycleCompat()
                }
            }
        }
        // Compose toolbars (Firefox 15x): the tag is only visible as the node's id name while walking the tree.
        if (ids.any { !it.contains(":id/") }) findNodeByIdName(root, ids)?.let { n ->
            try {
                readUrlNode(n)?.let { return it }
            } finally {
                n.recycleCompat()
            }
        }
        return null // known browser but its bar is not on screen (or shows a search / new tab)
    }

    /** Interprets one address-bar node. Null when it shows nothing URL-like (hint text, a search, a new tab). */
    private fun readUrlNode(n: AccessibilityNodeInfo): AddressBar? {
        if (n.isFocused && n.isEditable) return AddressBar(null, editing = true)
        val showingHint = Build.VERSION.SDK_INT >= 26 && n.isShowingHintText
        val text = if (showingHint) null else n.text?.toString()?.takeIf { it.isNotBlank() }
        // Text wins. Only a bar with no text at all (Compose toolbars) is read through its description, so a
        // search query typed into Chrome's bar can never be mistaken for a visit.
        val host = if (text != null) Domains.hostFromAddressBar(text)
        else n.contentDescription?.toString()?.let { Domains.hostFromAddressBarDescription(it) }
        return host?.let { AddressBar(it, editing = false) }
    }

    private fun findNodeByIdName(root: AccessibilityNodeInfo, ids: List<String>): AccessibilityNodeInfo? {
        var result: AccessibilityNodeInfo? = null
        val budget = intArrayOf(500)
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (result != null || depth > 20 || --budget[0] < 0 || isWebView(node)) return
            val name = node.viewIdResourceName
            if (name != null && ids.any { it == name || name.endsWith("/$it") }) { result = AccessibilityNodeInfo.obtain(node); return }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (result != null) return
            }
        }
        walk(root, 0)
        return result
    }

    private fun isWebView(node: AccessibilityNodeInfo): Boolean = node.className?.toString()?.endsWith("WebView") == true

    /** Unknown browsers: an EditText (or clickable TextView) whose whole text is a URL, in the top or bottom 20%. */
    private fun findUrlFieldInEdges(root: AccessibilityNodeInfo): AddressBar? {
        val top = (screenHeight * 0.20f).toInt()
        val bottom = screenHeight - top
        var result: AddressBar? = null
        val budget = intArrayOf(400)
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (result != null || depth > 18 || --budget[0] < 0 || isWebView(node)) return
            node.getBoundsInScreen(rect)
            if (rect.top < top || rect.bottom > bottom) {
                val cls = node.className?.toString() ?: ""
                val candidate = cls.endsWith("EditText") || (cls.endsWith("TextView") && node.isClickable)
                if (candidate && rect.height() in 1..top) {
                    if (node.isFocused && cls.endsWith("EditText")) { result = AddressBar(null, editing = true); return }
                    val hint = Build.VERSION.SDK_INT >= 26 && node.isShowingHintText
                    val host = if (hint) null else node.text?.toString()?.let { Domains.hostFromAddressBar(it) }
                    if (host != null) { result = AddressBar(host, editing = false); return }
                }
            }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (result != null) return
            }
        }
        walk(root, 0)
        return result
    }

    private class StripHit(val host: String, val top: Int)

    /**
     * In-app browsers show the page's host in a strip at the top of the screen (or of their sheet). Finds a node
     * there whose whole text (or description) is a bare host / URL, never descending into web content.
     */
    private fun findHostInTopStrip(root: AccessibilityNodeInfo): StripHit? {
        val limitY = (screenHeight * STRIP_FRACTION).toInt()
        var result: StripHit? = null
        val budget = intArrayOf(300)
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (result != null || depth > 18 || --budget[0] < 0 || isWebView(node)) return
            node.getBoundsInScreen(rect)
            if (rect.top > limitY) return // this node and everything under it is lower on screen
            if (!(node.isEditable && node.isFocused) && rect.height() in 1..limitY) {
                val host = node.text?.toString()?.let { Domains.hostFromToolbarText(it) }
                    ?: node.contentDescription?.toString()?.let { Domains.hostFromToolbarText(it) }
                if (host != null) { result = StripHit(host, rect.top); return }
            }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (result != null) return
            }
        }
        walk(root, 0)
        return result
    }

    /** True when the window holds a web view at least [MIN_WEBVIEW_FRACTION] of the screen tall, starting at or below [minTop]. */
    private fun hasLargeWebViewBelow(root: AccessibilityNodeInfo, minTop: Int): Boolean {
        val minHeight = (screenHeight * MIN_WEBVIEW_FRACTION).toInt()
        var found = false
        val budget = intArrayOf(600)
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (found || depth > 22 || --budget[0] < 0) return
            if (isWebView(node)) {
                node.getBoundsInScreen(rect)
                if (rect.height() >= minHeight && rect.top >= minTop - 8) found = true
                return // web content itself is never inspected
            }
            for (i in 0 until minOf(node.childCount, 60)) {
                val c = node.getChild(i) ?: continue
                walk(c, depth + 1)
                c.recycleCompat()
                if (found) return
            }
        }
        walk(root, 0)
        return found
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
        Log.i(TAG, "unbound (enforcing=${enforcing?.host})")
        _connected.value = false
        stateJob?.cancel(); stateJob = null
        mainHandler.removeCallbacksAndMessages(null)
        enforcing = null
        endSiteSession(System.currentTimeMillis())
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        _connected.value = false
        stateJob?.cancel(); stateJob = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleCompat() {
        if (Build.VERSION.SDK_INT < 33) runCatching { recycle() }
    }
}
