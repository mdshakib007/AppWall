package io.github.mdshakib007.appwall.core

import io.github.mdshakib007.appwall.data.db.BlockItem

/**
 * "Time you got back" model. Honest and explainable:
 *  - If we measured how much you used something in the week before blocking it (baseline), we credit that
 *    daily average for every day it has been blocked, scaled by how much of the day the schedule covers.
 *  - Otherwise we credit a conservative 4 minutes per blocked attempt (about one short scroll session).
 */
object Savings {
    const val FALLBACK_MS_PER_ATTEMPT = 4 * 60_000L
    private const val DAY = 24 * 3600_000L

    fun forItem(item: BlockItem, attempts: Int, now: Long): Long {
        val blockedFor = (minOf(now, item.untilAt ?: now) - item.createdAt).coerceAtLeast(0)
        if (item.baselineDailyMs > 0) {
            val coverage = if (item.allDay) 1.0 else {
                val window = if (item.endMinute >= item.startMinute) item.endMinute - item.startMinute else (1440 - item.startMinute + item.endMinute)
                (window / 1440.0) * (Integer.bitCount(item.daysMask and 0x7F) / 7.0)
            }
            return (item.baselineDailyMs * (blockedFor.toDouble() / DAY) * coverage).toLong()
        }
        return attempts * FALLBACK_MS_PER_ATTEMPT
    }

    fun total(items: List<BlockItem>, attemptsByKey: Map<String, Int>, now: Long): Long =
        items.sumOf { forItem(it, attemptsByKey[it.type.name + ":" + it.key] ?: 0, now) }
}
