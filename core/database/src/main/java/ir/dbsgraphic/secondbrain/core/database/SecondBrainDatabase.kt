package ir.dbsgraphic.secondbrain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.entity.Item
import ir.dbsgraphic.secondbrain.core.database.entity.ItemLink
import ir.dbsgraphic.secondbrain.core.database.entity.Project

/**
 * The encrypted local database — the single source of truth for the whole
 * system. Opened through SQLCipher with a Keystore-sealed key; no plaintext
 * DB ever touches disk (Constitution §11).
 *
 * The FTS5 search table and the item_links join table arrive in later phases;
 * the version will bump with reviewable migrations (schemas are exported).
 */
@Database(
    entities = [Item::class, Project::class, ItemLink::class],
    version = 2,
    exportSchema = true,
)
abstract class SecondBrainDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun projectDao(): ProjectDao
    abstract fun itemLinkDao(): ItemLinkDao

    companion object {
        const val NAME = "second_brain.db"
    }
}
