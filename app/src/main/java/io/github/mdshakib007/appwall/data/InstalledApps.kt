package io.github.mdshakib007.appwall.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isBrowser: Boolean,
)

/** Lists launchable apps. Icons are loaded lazily per row (PackageManager caches them). */
class InstalledApps(private val context: Context) {
    private val pm: PackageManager get() = context.packageManager

    @Volatile private var cache: List<InstalledApp>? = null
    @Volatile private var browserCache: Set<String>? = null

    suspend fun all(force: Boolean = false): List<InstalledApp> = withContext(Dispatchers.IO) {
        cache?.takeUnless { force } ?: load().also { cache = it }
    }

    fun invalidate() { cache = null; browserCache = null }

    /** Packages that can open http links (real browsers plus some in-app browser shells). */
    fun browserPackages(): Set<String> {
        browserCache?.let { return it }
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val set = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL).map { it.activityInfo.packageName }.toHashSet()
        set += Catalog.browserUrlBarIds.keys
        browserCache = set
        return set
    }

    private fun load(): List<InstalledApp> {
        val self = context.packageName
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchable = pm.queryIntentActivities(launchIntent, 0).map { it.activityInfo.packageName }.toHashSet()
        val browsers = browserPackages()
        return pm.getInstalledApplications(0)
            .asSequence()
            .filter { it.packageName in launchable && it.packageName != self && it.packageName !in Catalog.neverBlock }
            .map {
                InstalledApp(
                    packageName = it.packageName,
                    label = it.loadLabel(pm).toString(),
                    isSystem = it.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    isBrowser = it.packageName in browsers,
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun icon(packageName: String): Drawable? = runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

    fun label(packageName: String): String =
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() }.getOrDefault(packageName)

    fun isInstalled(packageName: String): Boolean = runCatching { pm.getApplicationInfo(packageName, 0); true }.getOrDefault(false)
}
