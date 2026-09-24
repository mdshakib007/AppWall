package io.github.mdshakib007.appwall.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId

data class AppUsage(val packageName: String, val totalMs: Long)

/** Thin wrapper over UsageStatsManager. Everything is computed on demand, nothing is stored. */
class UsageProvider(private val context: Context) {
    private val usm: UsageStatsManager get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Foreground time per package between [from] and [to], computed from the raw event stream
     * (far more accurate than queryUsageStats, which double counts across day buckets).
     */
    fun foregroundTime(from: Long, to: Long): Map<String, Long> {
        if (!hasPermission()) return emptyMap()
        val result = HashMap<String, Long>()
        val open = HashMap<String, Long>()
        val events = runCatching { usm.queryEvents(from, to) }.getOrNull() ?: return emptyMap()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> open[pkg] = e.timeStamp
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = open.remove(pkg) ?: continue
                    result[pkg] = (result[pkg] ?: 0L) + (e.timeStamp - start).coerceAtLeast(0)
                }
            }
        }
        // Whatever is still open counts until `to`.
        for ((pkg, start) in open) result[pkg] = (result[pkg] ?: 0L) + (to - start).coerceAtLeast(0)
        return result
    }

    fun topApps(from: Long, to: Long, limit: Int = 10): List<AppUsage> =
        foregroundTime(from, to).entries
            .filter { it.key !in Catalog.neverBlock && it.value > 0 }
            .sortedByDescending { it.value }
            .take(limit)
            .map { AppUsage(it.key, it.value) }

    /** Average daily foreground time of [packageName] over the [days] full days before [now]. */
    fun averageDailyAppUsage(packageName: String, days: Int, now: Long): Long {
        if (!hasPermission()) return 0L
        val from = now - days * 24L * 3600_000
        val total = foregroundTime(from, now)[packageName] ?: return 0L
        return total / days
    }

    fun startOfDay(now: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Per-day totals for the last [days] days (oldest first). Each entry is total foreground ms for all apps. */
    fun dailyTotals(days: Int, now: Long, zone: ZoneId = ZoneId.systemDefault()): List<Pair<LocalDate, Long>> {
        if (!hasPermission()) return emptyList()
        val today = LocalDate.now(zone)
        return (days - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
            val to = minOf(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), now)
            day to foregroundTime(from, to).filterKeys { it !in Catalog.neverBlock }.values.sum()
        }
    }
}
