package ir.dbsgraphic.secondbrain.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ir.dbsgraphic.secondbrain.core.database.MIGRATION_1_2
import ir.dbsgraphic.secondbrain.core.database.SecondBrainDatabase
import ir.dbsgraphic.secondbrain.core.database.dao.ItemDao
import ir.dbsgraphic.secondbrain.core.database.dao.ItemLinkDao
import ir.dbsgraphic.secondbrain.core.database.dao.ProjectDao
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
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideItemDao(database: SecondBrainDatabase): ItemDao = database.itemDao()

    @Provides
    fun provideProjectDao(database: SecondBrainDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideItemLinkDao(database: SecondBrainDatabase): ItemLinkDao = database.itemLinkDao()
}
