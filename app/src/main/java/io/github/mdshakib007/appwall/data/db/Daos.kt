package io.github.mdshakib007.appwall.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockItemDao {
    @Query("SELECT * FROM block_items ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockItem>>

    @Query("SELECT * FROM block_items ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlockItem>

    @Query("SELECT * FROM block_items WHERE id = :id")
    fun observe(id: Long): Flow<BlockItem?>

    @Query("SELECT * FROM block_items WHERE id = :id")
    suspend fun get(id: Long): BlockItem?

    @Query("SELECT * FROM block_items WHERE type = :type AND `key` = :key")
    suspend fun find(type: BlockType, key: String): BlockItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: BlockItem): Long

    @Update
    suspend fun update(item: BlockItem)

    @Delete
    suspend fun delete(item: BlockItem)

    @Query("SELECT COUNT(*) FROM block_items")
    fun observeCount(): Flow<Int>
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startAt DESC LIMIT 1")
    fun observeLatest(): Flow<FocusSession?>

    @Query("SELECT * FROM focus_sessions ORDER BY startAt DESC LIMIT 1")
    suspend fun latest(): FocusSession?

    @Query("SELECT * FROM focus_sessions ORDER BY startAt DESC")
    fun observeAll(): Flow<List<FocusSession>>

    @Insert
    suspend fun insert(session: FocusSession): Long
}

@Dao
interface BlockAttemptDao {
    @Insert
    suspend fun insert(attempt: BlockAttempt)

    @Query("SELECT * FROM block_attempts WHERE at >= :since ORDER BY at DESC")
    fun observeSince(since: Long): Flow<List<BlockAttempt>>

    @Query("SELECT COUNT(*) FROM block_attempts WHERE at >= :since")
    fun observeCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_attempts WHERE type = :type AND `key` = :key")
    fun observeCountFor(type: BlockType, key: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_attempts")
    fun observeTotal(): Flow<Int>

    @Query("SELECT MAX(at) FROM block_attempts WHERE type = :type AND `key` = :key")
    suspend fun lastAttemptAt(type: BlockType, key: String): Long?

    @Query("DELETE FROM block_attempts WHERE at < :before")
    suspend fun prune(before: Long)
}

@Dao
interface SiteSessionDao {
    @Insert
    suspend fun insert(session: SiteSession)

    @Query("SELECT * FROM site_sessions WHERE endAt >= :since ORDER BY startAt DESC")
    fun observeSince(since: Long): Flow<List<SiteSession>>

    @Query("SELECT * FROM site_sessions WHERE endAt >= :since")
    suspend fun since(since: Long): List<SiteSession>

    @Query("SELECT COALESCE(SUM(endAt - startAt), 0) FROM site_sessions WHERE domain = :domain AND startAt >= :from AND startAt < :to")
    suspend fun totalFor(domain: String, from: Long, to: Long): Long

    @Query("DELETE FROM site_sessions WHERE endAt < :before")
    suspend fun prune(before: Long)
}
