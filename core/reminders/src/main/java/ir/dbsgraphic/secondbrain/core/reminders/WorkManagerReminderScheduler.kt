package ir.dbsgraphic.secondbrain.core.reminders

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.dbsgraphic.secondbrain.core.data.ReminderScheduler
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    override fun schedule(itemId: String, content: String, whenMillis: Long) {
        val delay = whenMillis - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putString(ReminderWorker.KEY_ITEM_ID, itemId)
            .putString(ReminderWorker.KEY_CONTENT, content)
            .build()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName(itemId), ExistingWorkPolicy.REPLACE, request)
    }

    override fun cancel(itemId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(itemId))
    }

    private fun workName(itemId: String) = "reminder_$itemId"
}
