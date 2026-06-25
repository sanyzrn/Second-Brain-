package ir.dbsgraphic.secondbrain.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import kotlinx.coroutines.flow.Flow

/**
 * Full-text search over the FTS5 table `items_fts` (created and kept in sync by
 * triggers in MIGRATION_2_3). Ranked with bm25 — best match first.
 *
 * `items_fts` is a virtual table Room doesn't model, so query verification is
 * skipped; the table is guaranteed to exist by the migration.
 */
@Dao
interface SearchDao {

    @SkipQueryVerification
    @Query(
        """
        SELECT i.* FROM items_fts
        JOIN items i ON i.id = items_fts.itemId
        WHERE items_fts MATCH :matchQuery AND i.status != 'trashed'
        ORDER BY bm25(items_fts)
        LIMIT :limit
        """,
    )
    fun search(matchQuery: String, limit: Int = 50): Flow<List<Item>>
}
