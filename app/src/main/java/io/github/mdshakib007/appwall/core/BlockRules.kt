package io.github.mdshakib007.appwall.core

import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.FocusSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Pure scheduling logic. */
object BlockRules {

    fun isFocusActive(session: FocusSession?, now: Long): Boolean =
        session != null && now >= session.startAt && now < session.endAt

    /** Is this item's own schedule active at [now] (ignoring focus mode)? */
    fun isActive(item: BlockItem, now: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        item.untilAt?.let { if (now >= it) return false }
        if (item.allDay && item.daysMask == BlockItem.ALL_DAYS) return true
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val minute = ldt.hour * 60 + ldt.minute
        val today = ldt.dayOfWeek
        if (item.allDay) return dayEnabled(item.daysMask, today)
        val start = item.startMinute
        val end = item.endMinute
        return if (start <= end) {
            dayEnabled(item.daysMask, today) && minute >= start && minute < end
        } else {
            // wraps midnight: e.g. 22:00 -> 06:00. Before midnight belongs to today, after belongs to yesterday.
            (dayEnabled(item.daysMask, today) && minute >= start) ||
                (dayEnabled(item.daysMask, today.minus(1)) && minute < end)
        }
    }

    fun dayEnabled(mask: Int, day: DayOfWeek): Boolean = (mask shr (day.value - 1)) and 1 == 1

    fun hasEnded(item: BlockItem, now: Long): Boolean = item.untilAt != null && now >= item.untilAt

    /** Combined verdict: focus mode blocks everything; otherwise the item's own schedule decides. */
    fun isBlocked(item: BlockItem, focus: FocusSession?, now: Long): Boolean =
        isFocusActive(focus, now) || isActive(item, now)
}
