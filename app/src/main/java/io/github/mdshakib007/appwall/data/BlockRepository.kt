package io.github.mdshakib007.appwall.data

import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.data.db.AppWallDatabase
import io.github.mdshakib007.appwall.data.db.BlockAttempt
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.data.db.FocusSession
import io.github.mdshakib007.appwall.data.db.SiteSession
import kotlinx.coroutines.flow.Flow

class LockedException : IllegalStateException("Focus Mode is active; blocks cannot be removed or relaxed.")

class BlockRepository(private val db: AppWallDatabase, private val usage: UsageProvider) {

    val items: Flow<List<BlockItem>> = db.blockItems().observeAll()
    val focus: Flow<FocusSession?> = db.focusSessions().observeLatest()
    val focusHistory: Flow<List<FocusSession>> = db.focusSessions().observeAll()

    fun item(id: Long): Flow<BlockItem?> = db.blockItems().observe(id)

    suspend fun isFocusActive(now: Long = System.currentTimeMillis()) =
        BlockRules.isFocusActive(db.focusSessions().latest(), now)

    suspend fun exists(type: BlockType, key: String) = db.blockItems().find(type, key) != null

    /** Adds an item; returns the existing one if already present. Computes a usage baseline for "time saved". */
    suspend fun add(type: BlockType, key: String, label: String): BlockItem {
        db.blockItems().find(type, key)?.let { return it }
        val now = System.currentTimeMillis()
        val baseline = when (type) {
            BlockType.APP -> usage.averageDailyAppUsage(key, days = 7, now = now)
            BlockType.WEBSITE -> {
                val week = 7L * 24 * 3600_000
                db.siteSessions().totalFor(key, now - week, now) / 7
            }
        }
        val item = BlockItem(type = type, key = key, label = label, createdAt = now, baselineDailyMs = baseline)
        val id = db.blockItems().insert(item)
        return item.copy(id = id)
    }

    /** Update schedule. Under Focus Mode the schedule is irrelevant (everything blocked) but edits are still refused
     *  so the user cannot pre-arrange an early escape. */
    suspend fun updateSchedule(item: BlockItem) {
        if (isFocusActive()) throw LockedException()
        db.blockItems().update(item)
    }

    suspend fun remove(item: BlockItem) {
        if (isFocusActive()) throw LockedException()
        db.blockItems().delete(item)
    }

    suspend fun startFocus(days: Int): FocusSession {
        require(days in 1..3650)
        val now = System.currentTimeMillis()
        if (isFocusActive(now)) throw IllegalStateException("Focus already active")
        val count = db.blockItems().getAll().size
        val session = FocusSession(startAt = now, endAt = now + days * 24L * 3600_000, days = days, itemsAtStart = count)
        val id = db.focusSessions().insert(session)
        return session.copy(id = id)
    }

    // --- Stats ---------------------------------------------------------------------------------

    suspend fun recordAttempt(item: BlockItem, now: Long = System.currentTimeMillis()) {
        // Debounce: the accessibility service can fire several events per second for one open.
        val last = db.attempts().lastAttemptAt(item.type, item.key)
        if (last != null && now - last < 3_000) return
        db.attempts().insert(BlockAttempt(type = item.type, key = item.key, label = item.displayName, at = now))
    }

    suspend fun recordSiteSession(domain: String, startAt: Long, endAt: Long) {
        if (endAt - startAt < 1_000) return
        db.siteSessions().insert(SiteSession(domain = domain, startAt = startAt, endAt = endAt))
    }

    fun attemptsSince(since: Long): Flow<List<BlockAttempt>> = db.attempts().observeSince(since)
    fun attemptCountSince(since: Long): Flow<Int> = db.attempts().observeCountSince(since)
    fun attemptCountFor(type: BlockType, key: String): Flow<Int> = db.attempts().observeCountFor(type, key)
    fun totalAttempts(): Flow<Int> = db.attempts().observeTotal()
    fun siteSessionsSince(since: Long): Flow<List<SiteSession>> = db.siteSessions().observeSince(since)
    suspend fun siteSessionsSinceOnce(since: Long): List<SiteSession> = db.siteSessions().since(since)

    suspend fun pruneOld(now: Long = System.currentTimeMillis()) {
        val keep = 90L * 24 * 3600_000
        db.attempts().prune(now - keep)
        db.siteSessions().prune(now - keep)
    }
}
