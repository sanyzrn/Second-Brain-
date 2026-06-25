package ir.dbsgraphic.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemLinkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: ItemLink)

    @Query("DELETE FROM item_links WHERE fromId = :fromId AND toId = :toId AND kind = :kind")
    suspend fun delete(fromId: String, toId: String, kind: String)

    /** Backlinks: the Items that point AT :id — the question §5 must answer. */
    @Query(
        """
        SELECT i.* FROM items i
        INNER JOIN item_links l ON i.id = l.fromId
        WHERE l.toId = :id AND i.status != 'trashed'
        ORDER BY i.createdAt DESC
        """,
    )
    fun observeBacklinks(id: String): Flow<List<Item>>

    /** Outgoing: the Items that :id points at. */
    @Query(
        """
        SELECT i.* FROM items i
        INNER JOIN item_links l ON i.id = l.toId
        WHERE l.fromId = :id AND i.status != 'trashed'
        ORDER BY i.createdAt DESC
        """,
    )
    fun observeOutgoing(id: String): Flow<List<Item>>

    @Query("SELECT * FROM item_links")
    suspend fun getAll(): List<ItemLink>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(links: List<ItemLink>)
}
