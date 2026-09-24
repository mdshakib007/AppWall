package io.github.mdshakib007.appwall.core

import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.FocusSession

/**
 * Immutable snapshot the services read synchronously on hot paths (every accessibility event, every DNS query).
 * Rebuilt whenever items / focus change or the minute ticks over.
 */
class BlockState(
    val items: List<BlockItem>,
    val focus: FocusSession?,
    val now: Long,
) {
    val focusActive: Boolean = BlockRules.isFocusActive(focus, now)

    /** Package names blocked right now. */
    val blockedPackages: Set<String> =
        items.filter { it.type == io.github.mdshakib007.appwall.data.db.BlockType.APP && BlockRules.isBlocked(it, focus, now) }
            .map { it.key }.toHashSet()

    /** Domains blocked right now (subdomains implied). */
    val blockedDomains: List<String> =
        items.filter { it.type == io.github.mdshakib007.appwall.data.db.BlockType.WEBSITE && BlockRules.isBlocked(it, focus, now) }
            .map { it.key }

    val itemByPackage: Map<String, BlockItem> =
        items.filter { it.type == io.github.mdshakib007.appwall.data.db.BlockType.APP }.associateBy { it.key }
    val itemByDomain: Map<String, BlockItem> =
        items.filter { it.type == io.github.mdshakib007.appwall.data.db.BlockType.WEBSITE }.associateBy { it.key }

    fun blockedItemForPackage(pkg: String): BlockItem? = if (pkg in blockedPackages) itemByPackage[pkg] else null

    fun blockedItemForHost(host: String): BlockItem? =
        Domains.findBlocked(host, blockedDomains)?.let { itemByDomain[it] }

    companion object {
        val EMPTY = BlockState(emptyList(), null, 0L)
    }
}
