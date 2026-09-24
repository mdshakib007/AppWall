package io.github.mdshakib007.appwall.core

import io.github.mdshakib007.appwall.data.db.BlockItem
import io.github.mdshakib007.appwall.data.db.BlockType
import io.github.mdshakib007.appwall.data.db.FocusSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class BlockRulesTest {
    private val zone = ZoneId.of("Asia/Dhaka")
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int) = LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()
    private fun item(
        untilAt: Long? = null, allDay: Boolean = true, days: Int = BlockItem.ALL_DAYS, start: Int = 0, end: Int = 1440,
    ) = BlockItem(type = BlockType.APP, key = "x", label = "x", createdAt = 0, untilAt = untilAt, allDay = allDay, daysMask = days, startMinute = start, endMinute = end)

    // 2026-09-21 is a Monday.
    private val mon10 = at(2026, 9, 21, 10, 0)
    private val sat10 = at(2026, 9, 26, 10, 0)

    @Test fun alwaysBlocked() { assertTrue(BlockRules.isActive(item(), mon10, zone)) }

    @Test fun untilExpires() {
        assertTrue(BlockRules.isActive(item(untilAt = mon10 + 1), mon10, zone))
        assertFalse(BlockRules.isActive(item(untilAt = mon10), mon10, zone))
        assertTrue(BlockRules.hasEnded(item(untilAt = mon10), mon10))
    }

    @Test fun weekdaysOnly() {
        assertTrue(BlockRules.isActive(item(days = BlockItem.WEEKDAYS), mon10, zone))
        assertFalse(BlockRules.isActive(item(days = BlockItem.WEEKDAYS), sat10, zone))
    }

    @Test fun windowWithinDay() {
        val i = item(allDay = false, start = 9 * 60, end = 17 * 60)
        assertTrue(BlockRules.isActive(i, mon10, zone))
        assertFalse(BlockRules.isActive(i, at(2026, 9, 21, 17, 0), zone))
        assertFalse(BlockRules.isActive(i, at(2026, 9, 21, 8, 59), zone))
    }

    @Test fun windowWrapsMidnight() {
        val i = item(allDay = false, days = BlockItem.WEEKDAYS, start = 22 * 60, end = 6 * 60)
        assertTrue(BlockRules.isActive(i, at(2026, 9, 21, 23, 0), zone))   // Mon night
        assertTrue(BlockRules.isActive(i, at(2026, 9, 22, 2, 0), zone))    // Tue early (belongs to Mon)
        assertFalse(BlockRules.isActive(i, at(2026, 9, 22, 12, 0), zone))
        assertTrue(BlockRules.isActive(i, at(2026, 9, 26, 2, 0), zone))    // Sat 2am belongs to Fri night
        assertFalse(BlockRules.isActive(i, at(2026, 9, 27, 2, 0), zone))   // Sun 2am belongs to Sat night (not enabled)
    }

    @Test fun focusOverridesEverything() {
        val focus = FocusSession(startAt = mon10 - 1, endAt = mon10 + 1000, days = 1, itemsAtStart = 1)
        val expired = item(untilAt = mon10 - 1)
        assertTrue(BlockRules.isBlocked(expired, focus, mon10))
        assertFalse(BlockRules.isBlocked(expired, null, mon10))
        assertFalse(BlockRules.isFocusActive(focus, mon10 + 1000))
    }
}
