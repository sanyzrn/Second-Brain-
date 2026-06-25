package ir.dbsgraphic.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Items. Reads expose [Flow] so the UI is state-driven and reactive
 * (design spine). Writes are suspend functions.
 */
@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: Item)

    @Update
    suspend fun update(item: Item)

    /** The Inbox: formless, newest first (Constitution §3). */
    @Query("SELECT * FROM items WHERE status = 'inbox' ORDER BY createdAt DESC")
    fun observeInbox(): Flow<List<Item>>

    /** The Timeline spine: the whole life in chronological order (§19). */
    @Query("SELECT * FROM items WHERE status != 'trashed' ORDER BY createdAt DESC")
    fun observeTimeline(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observeById(id: String): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: String): Item?

    /** A Project's contents, derived by filtering Items — no duplication (§4). */
    @Query("SELECT * FROM items WHERE projectId = :projectId AND status != 'trashed' ORDER BY createdAt DESC")
    fun observeByProject(projectId: String): Flow<List<Item>>

    @Query("SELECT COUNT(*) FROM items WHERE status = 'inbox'")
    fun observeInboxCount(): Flow<Int>

    /** The recoverable Trash (Constitution §13). */
    @Query("SELECT * FROM items WHERE status = 'trashed' ORDER BY updatedAt DESC")
    fun observeTrashed(): Flow<List<Item>>

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM items WHERE status = 'trashed'")
    suspend fun deleteAllTrashed()

    // ── Backup (export/import everything) ───────────────────────────────────

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Item>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<Item>)
}
