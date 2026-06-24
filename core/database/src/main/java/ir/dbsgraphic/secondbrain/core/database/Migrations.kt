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
