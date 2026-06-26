package ir.dbsgraphic.secondbrain.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.dbsgraphic.secondbrain.core.data.Clock
import ir.dbsgraphic.secondbrain.core.data.IdGenerator
import ir.dbsgraphic.secondbrain.core.data.ItemRepository
import ir.dbsgraphic.secondbrain.core.data.ItemRepositoryImpl
import ir.dbsgraphic.secondbrain.core.data.HabitRepository
import ir.dbsgraphic.secondbrain.core.data.HabitRepositoryImpl
import ir.dbsgraphic.secondbrain.core.data.ProjectRepository
import ir.dbsgraphic.secondbrain.core.data.ProjectRepositoryImpl
import ir.dbsgraphic.secondbrain.core.data.SettingsRepository
import ir.dbsgraphic.secondbrain.core.data.SettingsRepositoryImpl
import ir.dbsgraphic.secondbrain.core.data.SystemClock
import ir.dbsgraphic.secondbrain.core.data.UuidGenerator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository

    @Binds
    @Singleton
    abstract fun bindProjectRepository(impl: ProjectRepositoryImpl): ProjectRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository

    companion object {
        @Provides
        fun provideClock(): Clock = SystemClock

        @Provides
        fun provideIdGenerator(): IdGenerator = UuidGenerator
    }
}
