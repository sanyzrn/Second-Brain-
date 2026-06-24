package ir.dbsgraphic.secondbrain.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: Projects (hubs) and the item_links connection table arrive.
 * Nothing in the Items table changes, so no data is touched (Constitution §9).
 * Structure matches the entities so Room's schema validation passes.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `projects` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "`color` TEXT, `status` TEXT NOT NULL, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_name` ON `projects` (`name`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `item_links` (" +
                "`fromId` TEXT NOT NULL, `toId` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, PRIMARY KEY(`fromId`, `toId`, `kind`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_links_toId` ON `item_links` (`toId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_links_fromId` ON `item_links` (`fromId`)")
    }
}

/**
 * v2 → v3: FTS5 search. A standalone FTS5 table holds a normalized copy of each
 * item's content; triggers keep it in sync with the items table. Persian
 * normalization is applied here via [PersianNormalizer.sqlExpression] — the same
 * folding the query side applies — so index and query always agree.
 *
 * Requires SQLCipher built with FTS5 (the modern net.zetetic builds enable it).
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE VIRTUAL TABLE IF NOT EXISTS `items_fts` USING fts5(" +
                "itemId UNINDEXED, norm, " +
                "tokenize = \"unicode61 remove_diacritics 2\")",
        )

        val normContent = PersianNormalizer.sqlExpression("content")
        val normNew = PersianNormalizer.sqlExpression("new.content")

        // Backfill existing items.
        db.execSQL("INSERT INTO `items_fts`(itemId, norm) SELECT id, $normContent FROM items")

        // Keep in sync via triggers (design spine).
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS items_fts_ai AFTER INSERT ON items BEGIN " +
                "INSERT INTO items_fts(itemId, norm) VALUES (new.id, $normNew); END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS items_fts_ad AFTER DELETE ON items BEGIN " +
                "DELETE FROM items_fts WHERE itemId = old.id; END",
        )
        db.execSQL(
            "CREATE TRIGGER IF NOT EXISTS items_fts_au AFTER UPDATE ON items BEGIN " +
                "UPDATE items_fts SET norm = $normNew WHERE itemId = new.id; END",
        )
    }
}
