package ir.dbsgraphic.secondbrain.core.reminders

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Fires a reminder notification. Content is passed via inputData (a snapshot at
 * schedule time), so the worker needs no DB/DI — the default WorkManager factory
 * can construct it.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {

    override fun doWork(): Result {
        val itemId = inputData.getString(KEY_ITEM_ID).orEmpty()
        val content = inputData.getString(KEY_CONTENT).orEmpty()
        if (content.isNotBlank()) {
            ReminderNotifications.show(applicationContext, itemId, content)
        }
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "itemId"
        const val KEY_CONTENT = "content"
    }
}
