package ir.dbsgraphic.secondbrain.core.reminders.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ir.dbsgraphic.secondbrain.core.data.ReminderScheduler
import ir.dbsgraphic.secondbrain.core.reminders.WorkManagerReminderScheduler
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler
}
