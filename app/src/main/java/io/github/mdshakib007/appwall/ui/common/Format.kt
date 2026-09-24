package io.github.mdshakib007.appwall.ui.common

import io.github.mdshakib007.appwall.core.BlockRules
import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.FocusSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

object Format {
    private val dateTime = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.getDefault())
    private val dateOnly = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    fun duration(ms: Long, compact: Boolean = false): String {
        val totalMin = abs(ms) / 60_000
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            ms < 60_000 -> if (compact) "0m" else "less than a minute"
            h == 0L -> "${m}m"
            h < 24 -> if (m == 0L) "${h}h" else "${h}h ${m}m"
            else -> {
                val d = h / 24
                val rh = h % 24
                if (rh == 0L) "${d}d" else "${d}d ${rh}h"
            }
        }
    }

    /** Human "in 3 days", "in 2h 10m", "ended". */
    fun remaining(untilAt: Long, now: Long): String {
        val left = untilAt - now
        if (left <= 0) return "ended"
        val min = left / 60_000
        return when {
            min < 60 -> "${min.coerceAtLeast(1)}m left"
            min < 24 * 60 -> "${min / 60}h ${min % 60}m left"
            else -> {
                val days = min / (24 * 60)
                val hours = (min % (24 * 60)) / 60
                if (days >= 30) "$days days left" else if (hours == 0L) "$days days left" else "${days}d ${hours}h left"
            }
        }
    }

    fun dateTime(epoch: Long): String = dateTime.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
    fun date(epoch: Long): String = dateOnly.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneId.systemDefault()))
    fun minuteOfDay(minute: Int): String = time.format(LocalDateTime.of(2000, 1, 1, (minute / 60) % 24, minute % 60))

    fun days(mask: Int): String = when (mask) {
        BlockItem.ALL_DAYS -> "Every day"
        BlockItem.WEEKDAYS -> "Weekdays"
        BlockItem.WEEKEND -> "Weekends"
        0 -> "No days"
        else -> DayOfWeek.entries.filter { BlockRules.dayEnabled(mask, it) }
            .joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

    /** One-line schedule summary for lists. */
    fun schedule(item: BlockItem, now: Long): String {
        val parts = ArrayList<String>(2)
        item.untilAt?.let { parts += if (now >= it) "Ended" else "Until ${date(it)}" }
        if (!item.allDay) {
            parts += "${days(item.daysMask)} ${minuteOfDay(item.startMinute)}–${minuteOfDay(item.endMinute)}"
        } else if (item.daysMask != BlockItem.ALL_DAYS) {
            parts += days(item.daysMask)
        }
        if (parts.isEmpty()) return "Always"
        return parts.joinToString(" · ")
    }

    fun focusRemaining(session: FocusSession, now: Long): String = remaining(session.endAt, now)
}
