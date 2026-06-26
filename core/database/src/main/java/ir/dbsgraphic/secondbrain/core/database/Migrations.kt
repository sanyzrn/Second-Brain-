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
        FtsSchema.createTableAndTriggers(db)
        FtsSchema.backfillMissing(db)
    }
}

/** v3 → v4: a scheduled reminder time on Items. No data touched (§9). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN reminderAt INTEGER")
    }
}

/** v4 → v5: habit check-ins (the Habits vertical). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `habit_checkins` (" +
                "`habitId` TEXT NOT NULL, `dayStart` INTEGER NOT NULL, `doneAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`habitId`, `dayStart`))",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_checkins_habitId` ON `habit_checkins` (`habitId`)")
    }
}

/** v5 → v6: a device-calendar event id on Items (Calendar sync). No data touched (§9). */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE items ADD COLUMN calendarEventId INTEGER")
    }
}
