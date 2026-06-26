package ir.dbsgraphic.secondbrain.core.data

/**
 * Schedules/cancels local reminder notifications. Implemented in :core:reminders
 * (WorkManager). Abstracted here so the repository stays platform-agnostic and
 * testable.
 */
interface ReminderScheduler {
    fun schedule(itemId: String, content: String, whenMillis: Long)
    fun cancel(itemId: String)
}

/** Default no-op (used in tests / when reminders aren't wired). */
object NoOpReminderScheduler : ReminderScheduler {
    override fun schedule(itemId: String, content: String, whenMillis: Long) = Unit
    override fun cancel(itemId: String) = Unit
}
