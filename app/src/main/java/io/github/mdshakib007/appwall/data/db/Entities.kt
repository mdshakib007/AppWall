package io.github.mdshakib007.appwall.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BlockType { APP, WEBSITE }

/**
 * One blocked app or website plus its schedule.
 *
 * Schedule model (two independent dimensions, together they cover hours/days/months/years/routines):
 *  - [untilAt]: null = forever, else epoch millis when the block ends.
 *  - [allDay] or a daily window [startMinute]..[endMinute] on [daysMask] (bit 0 = Monday ... bit 6 = Sunday).
 *    A window may wrap midnight (endMinute < startMinute).
 */
@Entity(
    tableName = "block_items",
    indices = [Index(value = ["type", "key"], unique = true)],
)
data class BlockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BlockType,
    /** Package name for apps, normalized domain for websites. */
    val key: String,
    val label: String,
    val createdAt: Long,
    val untilAt: Long? = null,
    val allDay: Boolean = true,
    val daysMask: Int = ALL_DAYS,
    val startMinute: Int = 0,
    val endMinute: Int = 24 * 60,
    /** Average daily usage in ms over the week before blocking (0 = unknown). Used for "time saved". */
    val baselineDailyMs: Long = 0,
) {
    /** Websites are always shown by their domain; apps by their launcher label. */
    val displayName: String get() = if (type == BlockType.WEBSITE) key else label

    companion object {
        const val ALL_DAYS = 0b1111111
        const val WEEKDAYS = 0b0011111
        const val WEEKEND = 0b1100000
    }
}

/** A global commitment: everything in the list is blocked, all day, until [endAt]. Cannot be cancelled. */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startAt: Long,
    val endAt: Long,
    val days: Int,
    val itemsAtStart: Int,
)

/** Every time AppWall stopped the user from opening something. */
@Entity(tableName = "block_attempts", indices = [Index("at")])
data class BlockAttempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BlockType,
    val key: String,
    val label: String,
    val at: Long,
)

/** Time spent with a website visible in a browser (derived from the address bar). */
@Entity(tableName = "site_sessions", indices = [Index("startAt"), Index("domain")])
data class SiteSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val startAt: Long,
    val endAt: Long,
)
