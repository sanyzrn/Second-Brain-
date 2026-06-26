package ir.dbsgraphic.secondbrain.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.dbsgraphic.secondbrain.core.database.FtsSchema
import ir.dbsgraphic.secondbrain.core.database.MIGRATION_1_2
import ir.dbsgraphic.secondbrain.core.database.MIGRATION_2_3
import ir.dbsgraphic.secondbrain.core.database.MIGRATION_3_4
import ir.dbsgraphic.secondbrain.core.database.SecondBrainDatabase
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
import ir.dbsgraphic.secondbrain.core.database.dao.SearchDao
import ir.dbsgraphic.secondbrain.core.security.DatabaseKeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyProvider: DatabaseKeyProvider,
    ): SecondBrainDatabase {
        // SQLCipher's native library must be loaded before opening the DB.
        System.loadLibrary("sqlcipher")

        val factory = SupportOpenHelperFactory(keyProvider.getOrCreateDatabaseKey())

        return Room.databaseBuilder(
            context,
            SecondBrainDatabase::class.java,
            SecondBrainDatabase.NAME,
        )
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            // The FTS5 table isn't a Room @Entity, so migrations alone miss the
            // fresh-install path. Create it on open (idempotent) and backfill any
            // items missing from the index — this also repairs older databases.
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    FtsSchema.createTableAndTriggers(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    FtsSchema.createTableAndTriggers(db)
                    FtsSchema.backfillMissing(db)
                }
            })
            .build()
    }

    @Provides
    fun provideItemDao(database: SecondBrainDatabase): ItemDao = database.itemDao()

    @Provides
    fun provideProjectDao(database: SecondBrainDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideItemLinkDao(database: SecondBrainDatabase): ItemLinkDao = database.itemLinkDao()

    @Provides
    fun provideSearchDao(database: SecondBrainDatabase): SearchDao = database.searchDao()
}
